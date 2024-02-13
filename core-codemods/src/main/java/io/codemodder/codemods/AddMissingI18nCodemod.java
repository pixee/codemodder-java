package io.codemodder.codemods;

import com.google.common.annotations.VisibleForTesting;
import io.codemodder.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.translate.TranslateClient;
import software.amazon.awssdk.services.translate.model.*;

/**
 * This codemod finds missing i18n keys in property files and adds them to the file, using an LLM to
 * generate the missing values.
 */
@Codemod(
    id = "pixee:java/missing-i18n",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class AddMissingI18nCodemod extends RawFileChanger {

  private final TranslateClient translateClient;
  private final List<Language> languagesAvailable;

  @Inject
  public AddMissingI18nCodemod(final TranslateClient translateClient) {
    this.translateClient = Objects.requireNonNull(translateClient);
    ListLanguagesResponse languagesResponse =
        translateClient.listLanguages(ListLanguagesRequest.builder().build());
    this.languagesAvailable = putInPreferredOrder(languagesResponse.languages());
  }

  private List<Language> putInPreferredOrder(final List<Language> languages) {
    List<Language> orderedLanguages = new ArrayList<>(languages);
    orderedLanguages.removeIf(lang -> preferredTranslationSources.contains(lang.languageCode()));
    preferredTranslationSources.forEach(
        lang ->
            orderedLanguages.add(
                0,
                languages.stream().filter(l -> l.languageCode().equals(lang)).findFirst().get()));
    return orderedLanguages;
  }

  @Override
  public List<CodemodChange> visitFile(final CodemodInvocationContext context) throws IOException {
    Path path = context.path();
    String fileName = path.getFileName().toString();
    Optional<String> prefix = getPropertyFilePrefix(fileName);
    if (prefix.isEmpty()) {
      // doesn't look like a i18n properties file
      return List.of();
    }
    return doVisitFile(context, path, prefix.get());
  }

  /** If it's a property file, return the prefix, otherwise return {@link Optional#empty()}. */
  @VisibleForTesting
  static Optional<String> getPropertyFilePrefix(final String fileName) {
    Matcher countryMatcher = PROPERTY_FILE_WITH_COUNTRY.matcher(fileName);
    if (countryMatcher.matches()) {
      String prefix = countryMatcher.group(1);
      return Optional.of(prefix);
    }
    Matcher noCountryMatcher = PROPERTY_FILE_WITHOUT_COUNTRY.matcher(fileName);
    if (noCountryMatcher.matches()) {
      String prefix = noCountryMatcher.group(1);
      return Optional.of(prefix);
    }
    return Optional.empty();
  }

  /**
   * Perform the actual inspection and changes.
   *
   * @param context the context
   * @param path the path to the file being checked
   * @param filePrefix the prefix of the file name, e.g. "messages" for "messages_en.properties"
   */
  private List<CodemodChange> doVisitFile(
      final CodemodInvocationContext context, final Path path, final String filePrefix)
      throws IOException {
    // try to load it as a properties file and make sure that works
    Properties properties = new Properties();
    String charset = UniversalDetector.detectCharset(path);
    properties.load(new InputStreamReader(Files.newInputStream(path), Charset.forName(charset)));

    List<Path> siblings = getSiblings(path, filePrefix);

    // if there's no siblings, there's nothing to compare our keys against
    if (siblings.isEmpty()) {
      LOG.trace("Have no baseline files to compare against, exiting");
      return List.of();
    }

    // find all the keys that are present in other sibling files, but not our file
    Set<PossiblyMissingKey> ourMissingOrEmptyKeys =
        findMissingOrEmptyKeys(path, properties, filePrefix, siblings);

    // of the keys we appear to missing, filter down to those for which we can find text references
    // to in the proj
    List<MissingKey> missingKeys = findUsedKeys(context, ourMissingOrEmptyKeys, siblings);

    // if we can't find any reference to the keys, maybe it's the _other_ properties files who have
    // it wrong
    if (missingKeys.isEmpty()) {
      LOG.debug("Missing keys in {} weren't discovered in the project", path);
      return List.of();
    }

    // we now have a set of keys that are referenced in the project and missing from our file
    List<KeyReplacement> keyReplacements = new ArrayList<>();
    for (MissingKey missingKey : missingKeys) {
      DefinitionReference definitionReference =
          getPreferredDefinitionForTranslation(missingKey.definitionReferences);
      TranslateTextRequest request =
          TranslateTextRequest.builder()
              .sourceLanguageCode(definitionReference.languageCode)
              .targetLanguageCode(missingKey.languageCode)
              .text(definitionReference.value)
              .build();
      TranslateTextResponse translateTextResponse = translateClient.translateText(request);
      String translatedText = translateTextResponse.translatedText();
      keyReplacements.add(
          new KeyReplacement(missingKey.key, translatedText, missingKey.usageReference));
    }

    // now we have a list of keys and their new values, we can apply them to the file
    List<CodemodChange> changes = new ArrayList<>();
    List<String> newLines = new ArrayList<>();
    List<KeyReplacement> replacementsToAddAtEnd = new ArrayList<>(keyReplacements);
    int lineNumber = 1;
    try (Stream<String> lines = Files.lines(path)) {
      for (String line : (Iterable<String>) lines::iterator) {
        for (KeyReplacement keyReplacement : keyReplacements) {
          String key = keyReplacement.key;
          String newValue = keyReplacement.newValue;
          List<UsageReference> usageReferences = keyReplacement.usageReference;
          Matcher m = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*=.*$").matcher(line);
          // we've identified the line we should replace, it must have been empty
          if (m.matches()) {
            LOG.debug("Replacing {} with {}", key, newValue);
            newLines.add(key + "=" + newValue);
            String description = createChangeDescription(key, usageReferences);
            changes.add(CodemodChange.from(lineNumber, description));
            replacementsToAddAtEnd.remove(keyReplacement);
          } else {
            newLines.add(line);
          }
        }
        lineNumber++;
      }
    }

    // these keys were missing entirely, so we should add them to the end
    for (KeyReplacement keyReplacement : replacementsToAddAtEnd) {
      newLines.add(keyReplacement.key + "=" + keyReplacement.newValue);
      String description =
          createChangeDescription(keyReplacement.key, keyReplacement.usageReference);
      changes.add(CodemodChange.from(lineNumber, description));
      lineNumber++; // need to incrementing this not-yet-real line number so we can have different
      // line numbers for each change
    }

    if (!changes.isEmpty()) {
      Files.write(path, newLines);
    }

    return changes;
  }

  private static String createChangeDescription(
      final String key, final List<UsageReference> usageReferences) {
    String descriptionTemplate =
        """
        Added missing i18n key value for "%s". The new value was based on other other property files that had values for the key.
        This key was confirmed to be in use in  %d place(s), including:

        %s
        """;
    String usages =
        usageReferences.stream()
            .limit(3)
            .map(ref -> "  " + ref.path + ": " + ref.line)
            .collect(Collectors.joining("\n"));

    return String.format(descriptionTemplate, key, usageReferences.size(), usages);
  }

  /**
   * For the source translation, prefer one of the most common languages spoken in the world. The
   * list was auto-generated by Copilot, so forgive any oversights. The idea is that the most common
   * languages may have the most reliable translations, rather than a more obscure language, which
   * may itself be a translation of a translation of a translation.
   */
  private DefinitionReference getPreferredDefinitionForTranslation(
      final List<DefinitionReference> definitions) {
    for (Language language : languagesAvailable) {
      Optional<DefinitionReference> definition =
          definitions.stream()
              .filter(d -> d.languageCode.equals(language.languageCode()))
              .findFirst();
      if (definition.isPresent()) {
        return definition.get();
      }
    }
    return definitions.get(0);
  }

  private record KeyReplacement(String key, String newValue, List<UsageReference> usageReference) {
    private KeyReplacement {
      Objects.requireNonNull(key);
      Objects.requireNonNull(newValue);
      Objects.requireNonNull(usageReference);
    }
  }

  private record PossiblyMissingKey(
      String languageCode, String key, List<DefinitionReference> definitionReferences) {
    private PossiblyMissingKey {
      Objects.requireNonNull(languageCode);
      Objects.requireNonNull(key);
      Objects.requireNonNull(definitionReferences);
    }
  }

  private record MissingKey(
      String languageCode,
      String key,
      List<DefinitionReference> definitionReferences,
      List<UsageReference> usageReference) {
    private MissingKey {
      Objects.requireNonNull(languageCode);
      Objects.requireNonNull(key);
      Objects.requireNonNull(definitionReferences);
      Objects.requireNonNull(usageReference);
    }
  }

  /** Find all the keys that are defined in other sibling files, but not in this file. */
  private Set<PossiblyMissingKey> findMissingOrEmptyKeys(
      final Path path,
      final Properties myProperties,
      final String filePrefix,
      final List<Path> siblings)
      throws IOException {
    Map<String, List<DefinitionReference>> missingKeyDefinitions = new HashMap<>();
    for (Path sibling : siblings) {
      Properties siblingProperties = new Properties();
      String charset = UniversalDetector.detectCharset(sibling);
      siblingProperties.load(new InputStreamReader(Files.newInputStream(sibling), charset));
      for (String siblingKey : siblingProperties.stringPropertyNames()) {
        if (!myProperties.containsKey(siblingKey)
            || myProperties.getProperty(siblingKey).isEmpty()) {
          String siblingValue = siblingProperties.getProperty(siblingKey);
          if (!siblingValue.isEmpty()) {
            String siblingFilename = sibling.getFileName().toString();
            List<DefinitionReference> definitionReferences =
                missingKeyDefinitions.computeIfAbsent(siblingKey, k -> new ArrayList<>());
            String sourceLanguageCode =
                siblingFilename.substring(filePrefix.length() + 1, filePrefix.length() + 3);
            definitionReferences.add(
                new DefinitionReference(sourceLanguageCode, siblingFilename, siblingValue));
          }
        }
      }
    }

    String targetLanguageCode =
        path.getFileName().toString().substring(filePrefix.length() + 1, filePrefix.length() + 3);
    return missingKeyDefinitions.entrySet().stream()
        .map(e -> new PossiblyMissingKey(targetLanguageCode, e.getKey(), e.getValue()))
        .collect(Collectors.toSet());
  }

  /** A definition of a key from another properties file. */
  private record DefinitionReference(String languageCode, String path, String value) {
    private DefinitionReference {
      Objects.requireNonNull(languageCode);
      Objects.requireNonNull(path);
      Objects.requireNonNull(value);
    }
  }

  /** A reference found in the wider project to key that is missing. */
  private record UsageReference(String path, String line) {
    private UsageReference {
      Objects.requireNonNull(path);
      Objects.requireNonNull(line);
    }
  }

  private List<MissingKey> findUsedKeys(
      final CodemodInvocationContext context,
      final Set<PossiblyMissingKey> ourMissingOrEmptyKeys,
      final List<Path> siblings)
      throws IOException {
    // loop through all files in projectDir recursively, ignoring binary file formats, and check if
    // the keys are referenced
    Path projectDir = context.codeDirectory().asPath();
    try (var paths = Files.walk(projectDir)) {
      try {
        return paths
            .filter(p -> !siblings.contains(p))
            .filter(p -> !context.path().equals(p))
            .filter(Files::isRegularFile)
            .filter(p -> !isObviouslyBinaryFile(p))
            .map(
                path -> {
                  List<String> lines;
                  try {
                    final String charset = UniversalDetector.detectCharset(path);
                    lines = Files.readString(path, Charset.forName(charset)).lines().toList();
                  } catch (IOException e) {
                    throw new UncheckedIOException(e);
                  }
                  for (String line : lines) {
                    for (PossiblyMissingKey key : ourMissingOrEmptyKeys) {
                      if (line.contains(key.key)) {
                        return Optional.of(
                            new MissingKey(
                                key.languageCode,
                                key.key,
                                key.definitionReferences,
                                List.of(new UsageReference(path.toString(), line))));
                      }
                    }
                  }
                  return Optional.<MissingKey>empty();
                })
            .flatMap(Optional::stream)
            .toList();
      } catch (UncheckedIOException e) {
        // unwrap stream's unchecked IOException
        throw e.getCause();
      }
    }
  }

  private List<Path> getSiblings(final Path path, final String prefix) throws IOException {
    Path parent = path.getParent();
    try (var paths = Files.list(parent)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(Files::isReadable)
          .filter(p -> p.getFileName().toString().startsWith(prefix))
          .filter(p -> !p.equals(path))
          .toList();
    }
  }

  /**
   * Returns true if the file at path is obviously a binary file. This is a heuristic based on the
   * file extension, and is not guaranteed to be correct, but should help massively in the
   * performance of 99% of cases. We could improve this further by doing some content-sniffing
   * checks.
   */
  private boolean isObviouslyBinaryFile(final Path path) {
    String fileName = path.getFileName().toString();
    String extension = FilenameUtils.getExtension(fileName).toLowerCase();
    return knownBinaryExtensions.contains(extension);
  }

  private static final Set<String> knownBinaryExtensions =
      Set.of(
          "ico", "jpg", "jpeg", "png", "gif", "svg", "tiff", "tif", "pdf", "bmp", "eps", "raw",
          "mp3", "mp4", "zip", "avi", "docx", "xlsx", "pptx", "jar", "dll", "com", "exe");

  private static final List<String> preferredTranslationSources =
      List.of("en", "de", "es", "fr", "it", "ja", "ko", "pt", "zh", "zh-TW");

  // create a pattern for matching property file names
  private static final Pattern PROPERTY_FILE_WITH_COUNTRY =
      Pattern.compile("(.*)_\\w{2}_\\w{2}\\.properties");
  private static final Pattern PROPERTY_FILE_WITHOUT_COUNTRY =
      Pattern.compile("(.*)_\\w{2}\\.properties");
  private static final Logger LOG = LoggerFactory.getLogger(AddMissingI18nCodemod.class);
}

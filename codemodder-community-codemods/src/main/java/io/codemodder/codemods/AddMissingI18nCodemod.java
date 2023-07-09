package io.codemodder.codemods;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import io.codemodder.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This codemod finds missing i18n keys in property files and adds them to the file, using an LLM to
 * generate the missing values.
 */
@Codemod(
    id = "pixee:java/missing-i18n",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class AddMissingI18nCodemod extends RawFileChanger {

  private final OpenAiService openAiService;

  @Inject
  public AddMissingI18nCodemod(final OpenAiService openAiService) {
    this.openAiService = Objects.requireNonNull(openAiService);
  }

  @Override
  public List<CodemodChange> visitFile(final CodemodInvocationContext context) {
    Path path = context.path();
    String fileName = path.getFileName().toString();
    Matcher m = PROPERTY_FILE_PATTERN.matcher(fileName);
    if (!m.matches()) {
      return List.of();
    }
    try {
      return doVisitFile(context, path, m.group(1));
    } catch (IOException e) {
      LOG.error("Problem collecting changes for {}", path, e);
    }
    return List.of();
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
    properties.load(Files.newInputStream(path));

    List<Path> siblings = getSiblings(path, filePrefix);

    // if there's no siblings, there's nothing to compare our keys against
    if (siblings.isEmpty()) {
      LOG.trace("Have no baseline files to compare against, exiting");
      return List.of();
    }

    // find all the keys that are present in other sibling files, but not our file
    Set<PossiblyMissingKey> ourMissingOrEmptyKeys = findMissingOrEmptyKeys(properties, siblings);

    // of the keys we appear to missing, filter down to those for which we can find text references
    // to in the proj
    List<MissingKey> missingKeys = findUsedKeys(context, ourMissingOrEmptyKeys);

    // if we can't find any reference to the keys, maybe it's the _other_ properties files who have
    // it wrong
    if (missingKeys.isEmpty()) {
      LOG.debug("Missing keys in {} weren't discovered in the project", path);
      return List.of();
    }

    /*
     * Here's what we have to send to the LLM:
     * - a few lines of context from this file to establish the language, tone, etc
     * - the key that is missing from this file
     * - the value of the key from other files to establish what the new value of the message should be
     */
    ObjectMapper mapper = new ObjectMapper();
    String contextLines = getFirstLines(path, 5, 250);

    // we now have a set of keys that are referenced in the project and missing from our file

    List<KeyReplacement> keyReplacements = new ArrayList<>();
    for (MissingKey missingKey : missingKeys) {
      String key = missingKey.key;
      List<DefinitionReference> definitionReferences = missingKey.definitionReferences;

      String otherDefinitions =
          definitionReferences.stream()
              .limit(5)
              .map(def -> "  " + def.path + ": " + key + "=" + def.value)
              .map(def -> def.length() > 250 ? def.substring(0, 250) + "..." : def)
              .collect(Collectors.joining("\n"));

      String promptTemplate =
          """
                We are helping fill in missing keys in a i18n resource/properties file. The key "%s" is missing
                from this file, but is present in other files in the project:

                %s

                To help you match the style and tone, consider a few lines of context from this file:
                ```
                %s
                ```

                What should the missing value for this key be (note that language code %s)? Don't say anything except the new value. If you have no idea what the value should be, say "NO IDEA". Match the tone and style of the existing values.
                """;

      String prompt =
          String.format(promptTemplate, key, otherDefinitions, contextLines, filePrefix);
      List<ChatMessage> messages = List.of(new ChatMessage("user", prompt));
      ChatCompletionRequest chatCompletionRequest =
          ChatCompletionRequest.builder()
              .model("gpt-3.5-turbo")
              .temperature(0D)
              .messages(messages)
              .build();
      ChatCompletionResult chatCompletion =
          openAiService.createChatCompletion(chatCompletionRequest);
      ChatCompletionChoice chatCompletionChoice = chatCompletion.getChoices().get(0);
      ChatMessage message = chatCompletionChoice.getMessage();
      String content = message.getContent();
      String newValue = content.substring(0, content.indexOf("\n"));
      keyReplacements.add(new KeyReplacement(key, newValue, missingKey.usageReference));
    }

    // now we have a list of keys and their new values, we can apply them to the file
    List<CodemodChange> changes = new ArrayList<>();
    List<String> newLines = new ArrayList<>();
    try (Stream<String> lines = Files.lines(path)) {
      int lineNumber = 1;
      for (String line : (Iterable<String>) lines::iterator) {
        for (KeyReplacement keyReplacement : keyReplacements) {
          String key = keyReplacement.key;
          String newValue = keyReplacement.newValue;
          List<UsageReference> usageReferences = keyReplacement.usageReference;
          Matcher m = Pattern.compile("^\\s*" + Pattern.quote(key) + "\\s*=.*$").matcher(line);
          if (m.matches()) {
            LOG.debug("Replacing {} with {}", key, newValue);
            newLines.add(key + "=" + newValue);
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

            String description =
                String.format(descriptionTemplate, key, usageReferences.size(), usages);
            changes.add(CodemodChange.from(lineNumber, description));
          } else {
            newLines.add(line);
          }
        }
        lineNumber++;
      }
    }

    if (!changes.isEmpty()) {
      Files.write(path, newLines);
    }

    return changes;
  }

  /** Find the first N non-empty lines of a file. */
  private String getFirstLines(final Path path, final int lines, final int maxChars)
      throws IOException {
    try (Stream<String> stream = Files.lines(path)) {
      return stream
          .filter(s -> !s.trim().isEmpty())
          .limit(lines)
          .map(s -> s.length() > maxChars ? s.substring(0, maxChars) + "..." : s)
          .collect(Collectors.joining("\n"));
    }
  }

  private record KeyReplacement(String key, String newValue, List<UsageReference> usageReference) {
    private KeyReplacement {
      Objects.requireNonNull(key);
      Objects.requireNonNull(newValue);
      Objects.requireNonNull(usageReference);
    }
  }

  private record PossiblyMissingKey(String key, List<DefinitionReference> definitionReferences) {
    private PossiblyMissingKey {
      Objects.requireNonNull(key);
      Objects.requireNonNull(definitionReferences);
    }
  }

  private record MissingKey(
      String key,
      List<DefinitionReference> definitionReferences,
      List<UsageReference> usageReference) {
    private MissingKey {
      Objects.requireNonNull(key);
      Objects.requireNonNull(definitionReferences);
      Objects.requireNonNull(usageReference);
    }
  }

  /** Find all the keys that are defined in other sibling files, but not in this file. */
  private Set<PossiblyMissingKey> findMissingOrEmptyKeys(
      final Properties myProperties, final List<Path> siblings) throws IOException {
    Map<String, List<DefinitionReference>> missingKeyDefinitions = new HashMap<>();
    for (Path sibling : siblings) {
      Properties siblingProperties = new Properties();
      siblingProperties.loadFromXML(Files.newInputStream(sibling));
      for (String siblingKey : siblingProperties.stringPropertyNames()) {
        if (!myProperties.containsKey(siblingKey)
            || myProperties.getProperty(siblingKey).isEmpty()) {
          String siblingValue = siblingProperties.getProperty(siblingKey);
          if (!siblingValue.isEmpty()) {
            String siblingFilename = sibling.getFileName().toString();
            List<DefinitionReference> definitionReferences =
                missingKeyDefinitions.computeIfAbsent(siblingKey, k -> new ArrayList<>());
            definitionReferences.add(new DefinitionReference(siblingFilename, siblingValue));
          }
        }
      }
    }

    return missingKeyDefinitions.entrySet().stream()
        .map(e -> new PossiblyMissingKey(e.getKey(), e.getValue()))
        .collect(Collectors.toSet());
  }

  /** A definition of a key from another properties file. */
  private record DefinitionReference(String path, String value) {
    private DefinitionReference {
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
      final CodemodInvocationContext context, final Set<PossiblyMissingKey> ourMissingOrEmptyKeys)
      throws IOException {
    // loop through all files in projectDir recursively, ignoring binary file formats, and check if
    // the keys are referenced
    Path projectDir = context.codeDirectory().asPath();
    List<MissingKey> missingAndUnusedKeys = new ArrayList<>();
    Files.walk(projectDir)
        .filter(Files::isRegularFile)
        .filter(p -> !isObviouslyBinaryFile(p))
        .forEach(
            p -> {
              try (Stream<String> lines = Files.lines(p)) {
                lines.forEach(
                    line -> {
                      for (PossiblyMissingKey key : ourMissingOrEmptyKeys) {
                        if (line.contains(key.key)) {
                          MissingKey missingKey =
                              new MissingKey(
                                  key.key,
                                  key.definitionReferences,
                                  List.of(new UsageReference(p.toString(), line)));
                          missingAndUnusedKeys.add(missingKey);
                          break;
                        }
                      }
                    });
              } catch (IOException ex) {
                throw new RuntimeException(ex);
              }
            });
    return List.copyOf(missingAndUnusedKeys);
  }

  private List<Path> getSiblings(final Path path, final String prefix) throws IOException {
    Path parent = path.getParent();
    List<Path> siblings = new ArrayList<>();
    Files.list(parent)
        .filter(Files::isRegularFile)
        .filter(Files::isReadable)
        .filter(p -> p.getFileName().toString().startsWith(prefix))
        .forEach(siblings::add);
    return List.copyOf(siblings);
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

  // create a pattern for matching property file names
  private static final Pattern PROPERTY_FILE_PATTERN = Pattern.compile("(.*_)\\w{2}\\.properties");
  private static final Logger LOG = LoggerFactory.getLogger(AddMissingI18nCodemod.class);
}

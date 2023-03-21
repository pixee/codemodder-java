package io.codemodder;

import com.github.javaparser.ast.CompilationUnit;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the entry point for codemod authors to invoke their codemods through our framework. Every
 * codemod should create a codemod then create a separate type with a main() method that invokes
 * this entry point.
 */
public final class CodemodInvoker {

  private final List<Changer> codemods;
  private final Path repositoryDir;
  private final List<IdentifiedChanger> changers;

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes, final Path repositoryDir) {
    this(codemodTypes, RuleContext.of(DefaultRuleSetting.ENABLED, List.of()), repositoryDir);
  }

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes,
      final RuleContext ruleContext,
      final Path repositoryDir) {

    // get all the providers ready for dependency injection & codemod instantiation
    List<CodemodProvider> providers =
        ServiceLoader.load(CodemodProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toUnmodifiableList());
    Set<AbstractModule> allModules = new HashSet<>();

    // add default module
    allModules.add(new CodeDirectoryModule(repositoryDir));

    // add all provider modules
    for (CodemodProvider provider : providers) {
      Set<AbstractModule> modules = provider.getModules(repositoryDir, codemodTypes);
      allModules.addAll(modules);
    }

    // record which changers are associated with which codemod ids
    List<IdentifiedChanger> changers = new ArrayList<>();

    // validate and instantiate the codemods
    Injector injector = Guice.createInjector(allModules);
    Set<String> codemodIds = new HashSet<>();
    List<Changer> codemods = new ArrayList<>();
    for (Class<? extends Changer> type : codemodTypes) {
      Codemod codemodAnnotation = type.getAnnotation(Codemod.class);
      validateRequiredFields(codemodAnnotation);
      Changer changer = injector.getInstance(type);
      String codemodId = codemodAnnotation.id();
      if (codemodIds.contains(codemodId)) {
        throw new UnsupportedOperationException("multiple codemods under id: " + codemodId);
      }
      codemodIds.add(codemodId);
      if (ruleContext.isRuleAllowed(codemodId)) {
        codemods.add(changer);
        changers.add(new IdentifiedChanger(codemodId, changer));
      }
    }

    this.changers = Collections.unmodifiableList(changers);
    this.codemods = Collections.unmodifiableList(codemods);
    this.repositoryDir = Objects.requireNonNull(repositoryDir);
  }

  private static final class IdentifiedChanger {
    final String id;
    final Changer changer;

    private IdentifiedChanger(final String id, final Changer changer) {
      this.id = id;
      this.changer = changer;
    }
  }

  /**
   * Run the codemods we've collected on the given file.
   *
   * @param cu the parsed JavaParser representation of the file
   * @param context a model we should keep updating as we process the file
   */
  public void execute(final Path path, final CompilationUnit cu, final FileWeavingContext context) {
    List<JavaParserChanger> javaParserChangers =
        codemods.stream()
            .filter(changer -> changer instanceof JavaParserChanger)
            .map(changer -> (JavaParserChanger) changer)
            .collect(Collectors.toUnmodifiableList());
    for (JavaParserChanger changer : javaParserChangers) {
      CodemodInvocationContext invocationContext =
          new DefaultCodemodInvocationContext(
              new DefaultCodeDirectory(repositoryDir),
              path,
              changers.stream().filter(ic -> ic.changer == changer).findFirst().orElseThrow().id,
              context);
      changer.visit(invocationContext, cu);
    }
  }

  private static class XMLEventChangerContext {
    private XMLEventChangerContext(
        XMLEventElementChanger changer, CodemodInvocationContext context) {
      this.changer = Objects.requireNonNull(changer);
      this.context = Objects.requireNonNull(context);
    }

    XMLEventElementChanger changer;
    CodemodInvocationContext context;
  }

  /**
   * Run the codemods we've collected on the given XML file. Note that for any given event, the
   * first {@link XMLEventElementChanger} to act on the event "wins", and no one else will have a
   * chance to operate on it. This limits how much multiple codemods can operate on the same
   * elements in a document.
   *
   * @param path the path to an XML file
   * @param context a model we should keep updating as we process the file
   * @return the modified XML contents, if changed at all
   */
  public Optional<ChangedFile> executeXmlFile(final Path path, final FileWeavingContext context)
      throws XMLStreamException, IOException {
    XMLInputFactory inputFactory = XMLInputFactory.newFactory();
    XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
    StringWriter sw = new StringWriter();

    String xml = Files.readString(path);
    StringReader reader = new StringReader(xml);
    final var xmlReader = inputFactory.createXMLEventReader(reader);
    final var xmlWriter = outputFactory.createXMLEventWriter(sw);

    List<XMLEventChangerContext> xmlChangers =
        codemods.stream()
            .filter(changer -> changer instanceof XMLEventElementChanger)
            .map(changer -> (XMLEventElementChanger) changer)
            .map(
                changer -> {
                  CodemodInvocationContext invocationContext =
                      new DefaultCodemodInvocationContext(
                          new DefaultCodeDirectory(repositoryDir),
                          path,
                          changers.stream()
                              .filter(ic -> ic.changer == changer)
                              .findFirst()
                              .orElseThrow()
                              .id,
                          context);
                  return new XMLEventChangerContext(changer, invocationContext);
                })
            .collect(Collectors.toUnmodifiableList());

    while (xmlReader.hasNext()) {
      final var currentEvent = xmlReader.nextEvent();
      for (final XMLEventChangerContext changerContext : xmlChangers) {
        XMLEventElementChanger changer = changerContext.changer;
        CodemodInvocationContext codemodContext = changerContext.context;
        boolean changed =
            changer.onXmlEventRead(codemodContext, xmlReader, xmlWriter, currentEvent);
        if (!changed) {
          xmlWriter.add(currentEvent);
        } else {
          break; // a codemod changed this node, so we assume they cleaned up the state handling
        }
      }
    }

    reader.close();
    xmlReader.close();
    xmlWriter.close();

    if (context.weaves().isEmpty()) {
      return Optional.empty();
    }

    // transform the xml back to what they're expecting, starting by not including an <?xml> header
    // if they didn't have one
    String transformedXml = sw.toString();
    if (transformedXml.startsWith("<?xml") && !xml.startsWith("<?xml")) {
      transformedXml = transformedXml.substring(transformedXml.indexOf('>') + 1);
    }

    // remove the empty leftover lines affected by our changes if there are any
    Set<Integer> linesAffected =
        context.weaves().stream().map(Weave::lineNumber).collect(Collectors.toUnmodifiableSet());
    List<String> lines = transformedXml.lines().collect(Collectors.toUnmodifiableList());
    List<String> updatedLines = new ArrayList<>(lines.size() - linesAffected.size());
    for (int i = 1; i <= lines.size(); i++) {
      String actualLine = lines.get(i - 1);
      if (linesAffected.contains(i) && actualLine.isBlank()) {
        continue;
      }
      updatedLines.add(actualLine);
    }

    transformedXml = String.join("\n", updatedLines);

    // if the old file ended with a blank line, make sure to provide one
    if (xml.endsWith("\n") && !transformedXml.endsWith("\n")) {
      transformedXml += "\n";
    }

    Path modifiedFile = Files.createTempFile("codemod", ".xml");
    Files.write(modifiedFile, transformedXml.getBytes(StandardCharsets.UTF_8));
    ChangedFile changedFile =
        ChangedFile.createDefault(
            path.toFile().getAbsolutePath(), modifiedFile.toString(), context.weaves());
    return Optional.of(changedFile);
  }

  /**
   * This is the entry point custom-built codemods are supposed to go through. Right now, this is
   * not useful directly as we're worried primarily about the legacy entrypoints.
   */
  @SuppressWarnings("unused")
  public static void run(String[] args, final Class<? extends Changer>... codemodTypes) {
    CodemodInvoker invoker = new CodemodInvoker(Arrays.asList(codemodTypes), Path.of("."));
  }

  private static void validateRequiredFields(final Codemod codemodAnnotation) {
    String author = codemodAnnotation.author();
    if (StringUtils.isBlank(author)) {
      throw new IllegalArgumentException("must have an author");
    }

    String id = codemodAnnotation.id();
    if (!isValidCodemodId(id)) {
      throw new IllegalArgumentException("must have valid codemod id");
    }

    ReviewGuidance reviewGuidance = codemodAnnotation.reviewGuidance();
    if (reviewGuidance == null) {
      throw new IllegalArgumentException("must have review guidance");
    }
  }

  @VisibleForTesting
  static boolean isValidCodemodId(final String codemodId) {
    return codemodIdPattern.matcher(codemodId).matches();
  }

  private static final Pattern codemodIdPattern =
      Pattern.compile("^([A-Za-z0-9]+):([A-Za-z0-9]+)\\/([A-Za-z0-9\\-]+)$");

  private static final Logger log = LoggerFactory.getLogger(CodemodInvoker.class);
}

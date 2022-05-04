package io.pixee.codefixer.java.protections;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.google.common.annotations.VisibleForTesting;
import io.pixee.codefixer.java.ChangedFile;
import io.pixee.codefixer.java.FileBasedVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.Weave;
import io.pixee.codefixer.java.WeavingResult;
import io.pixee.security.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/** This is an important default weaver that will inject our dependency. */
public final class DependencyInjectingVisitor implements FileBasedVisitor {

  private final PomRewriterStrategy pomRewriterStrategy;
  private final WeavingResult emptyWeaveResult;
  private boolean scanned;
  private Set<File> pomsToUpdate;

  public DependencyInjectingVisitor() {
    this(new MavenXpp3RewriterStrategy());
  }

  DependencyInjectingVisitor(PomRewriterStrategy pomRewriterStrategy) {
    this.pomRewriterStrategy = Objects.requireNonNull(pomRewriterStrategy);
    this.emptyWeaveResult =
        WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());
    this.scanned = false;
    this.pomsToUpdate = Collections.emptySet();
  }

  @Override
  public String ruleId() {
    return pomInjectionRuleId;
  }

  /**
   * There are many different ways we could choose to rewrite the POM to contain our dependency.
   * There are many tradeoffs between them as to version coverage, accuracy, robustness,
   * "disruptiveness" of the patch, etc., so we keep multiple strategies around until we probably
   * have to hand-develop a solution that ticks all our boxes.
   */
  public interface PomRewriterStrategy {
    /**
     * Given a model of the POM, and the string contents itself, return the String version of the
     * POM, rewritten to contain our dependency.
     */
    String rewritePom(String pomContentsAsString, Model model) throws IOException;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Uses the {@link MavenXpp3Writer} to rewrite the {@link Model} with our dependency added in.
   */
  public static class MavenXpp3RewriterStrategy implements PomRewriterStrategy {
    @Override
    public String rewritePom(final String pomContentsAsString, final Model model)
        throws IOException {
      var writer = new MavenXpp3Writer();
      var rewrittenPom = new StringWriter();
      writer.write(rewrittenPom, model);
      return rewrittenPom.toString();
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>Uses an XSLT transformation to convert the POM to inject the dependency.
   *
   * <p>TODO: This doesn't work yet -- the XSL needs some love. It only correctly injects a new
   * dependencies block when there is none.
   */
  public static class XslTransformingStrategy implements PomRewriterStrategy {

    @Override
    public String rewritePom(final String pomContentsAsString, final Model model)
        throws IOException {
      var factory = TransformerFactory.newInstance();
      var injectionXsl =
          IOUtils.toString(
              new InputStreamReader(
                  Objects.requireNonNull(
                      getClass().getResourceAsStream("/inject-dependency.xsl"),
                      "dependency injection xsl not found")));
      var xslt = new StreamSource(new StringReader(injectionXsl));
      try {
        var transformer = factory.newTransformer(xslt);
        var text = new StreamSource(new StringReader(pomContentsAsString));
        var sw = new StringWriter();
        transformer.transform(text, new StreamResult(sw));
        final String dependenciesBlock = createDependency();
        var transformedText = sw.toString();
        transformedText = transformedText.replace("%PIXEE_DEPENDENCY_INFO%", dependenciesBlock);
        return transformedText;
      } catch (TransformerException e) {
        throw new IOException(e);
      }
    }

    private String createDependency() {
      StringBuilder sb = new StringBuilder();
      sb.append(nl);
      sb.append("    <dependencies>").append(nl);
      sb.append("      <dependency>").append(nl);
      sb.append("        <groupId>").append(projectGroup).append("</groupId>").append(nl);
      sb.append("        <artifactId>")
          .append(projectArtifactId)
          .append("</artifactId>")
          .append(nl);
      sb.append("        <version>").append(projectVersion).append("</version>").append(nl);
      sb.append("      </dependency>").append(nl);
      sb.append("    </dependencies>").append(nl);
      return sb.toString();
    }
  }

  @Override
  public WeavingResult visitRepositoryFile(
      final File repositoryRoot,
      final File file,
      final FileWeavingContext weavingContext,
      final Set<ChangedFile> changedJavaFiles) {

    if (!scanned) {
      scanned = true;
      try {
        this.pomsToUpdate = scan(repositoryRoot.getCanonicalFile(), changedJavaFiles);
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }

      LOG.info("Found the following poms to update:");
      pomsToUpdate.forEach(LOG::info);
    }

    if (pomsToUpdate.contains(file)) {
      LOG.info("Injecting dependency into: {}", file);
      try {
        var changedFile = transformPomIfNeeded(file);
        if (changedFile != null) {
          return WeavingResult.createDefault(Set.of(changedFile), Collections.emptySet());
        }
      } catch (Exception e) {
        LOG.error("Problem injecting pom", e);
        return WeavingResult.createDefault(Collections.emptySet(), Set.of(file.getAbsolutePath()));
      }
    }
    return emptyWeaveResult;
  }

  private Set<File> scan(final File repositoryRoot, final Set<ChangedFile> changedJavaFiles) {
    LOG.info(
        "Scanning repository root for all poms representing {} changed Java files",
        changedJavaFiles.size());
    final Set<File> pomsToUpdate = new HashSet<>();
    changedJavaFiles.forEach(
        changedFile -> {
          final var path = changedFile.originalFilePath();
          final var file = new File(path);
          final var aboveRepoDir = repositoryRoot.getParentFile().toPath();
          var parent = file.getParentFile();
          try {
            while (parent != null && !Files.isSameFile(parent.toPath(), aboveRepoDir)) {
              var potentialPom = new File(parent, "pom.xml");
              if (potentialPom.exists()) {
                LOG.info("Adding pom: {}", potentialPom);
                pomsToUpdate.add(potentialPom);
                parent = null;
              } else {
                parent = parent.getParentFile();
              }
            }
          } catch (IOException e) {
            LOG.error("Couldn't scan for pom files of changed Java source files", e);
          }
        });
    return pomsToUpdate;
  }

  @VisibleForTesting
  ChangedFile transformPomIfNeeded(final File file) throws IOException, XmlPullParserException {
    var pomContents = IOUtils.toString(new FileReader(file));
    var reader = new MavenXpp3Reader();
    var model = reader.read(new StringReader(pomContents));
    var hasDependencyAlready =
        model.getDependencies().stream()
            .anyMatch(
                dep ->
                    projectGroup.equals(dep.getGroupId())
                        && projectArtifactId.equals(dep.getArtifactId()));

    if (hasDependencyAlready) {
      LOG.info("Not weaving pom since it contained our dependency");
      return null;
    }

    var dependency = new Dependency();
    dependency.setArtifactId(projectArtifactId);
    dependency.setGroupId(projectGroup);
    dependency.setVersion(projectVersion);
    model.addDependency(dependency);

    var rewrittenPomContents = pomRewriterStrategy.rewritePom(pomContents, model);

    File modifiedPom = File.createTempFile("pom", ".xml");
    FileUtils.write(modifiedPom, rewrittenPomContents, StandardCharsets.UTF_8);

    /*
     * We have to calculate the line position to associate with this diff. This is already calculated when the final
     * report is built later, so we should refactor.
     *
     * This is also wasteful because it duplicates reading the both files' contents into memory.
     */
    List<String> original = Files.readAllLines(file.toPath());
    List<String> patched = Files.readAllLines(modifiedPom.toPath());
    Patch<String> patch = DiffUtils.diff(original, patched);
    if (patch.getDeltas().isEmpty()) {
      LOG.warn("For some reason there was no pom delta");
      return null;
    }

    AbstractDelta<String> delta = patch.getDeltas().get(0);
    int position = 1 + delta.getSource().getPosition();
    return ChangedFile.createDefault(
        file.getAbsolutePath(),
        modifiedPom.getAbsolutePath(),
        Weave.from(position, pomInjectionRuleId));
  }

  @VisibleForTesting static final String pomInjectionRuleId = "pixee:java/mvn-dependency-injection";
  @VisibleForTesting static final String projectArtifactId = "java-code-security-toolkit";
  @VisibleForTesting static final String projectGroup = "io.github.pixee";
  @VisibleForTesting static final String projectVersion = "0.0.2";
  private static final String nl = System.getProperty("line.separator");
  private static final Logger LOG = LogManager.getLogger(DependencyInjectingVisitor.class);
}

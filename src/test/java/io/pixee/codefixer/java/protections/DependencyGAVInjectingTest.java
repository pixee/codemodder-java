package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.DependencyInjectingVisitor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

import io.pixee.codefixer.java.ChangedFile;
import io.pixee.codefixer.java.DependencyGAV;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.Weave;
import io.pixee.codefixer.java.WeavingResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

final class DependencyGAVInjectingTest {

  private DependencyInjectingVisitor weaver;

  @Test
  void it_scans_to_repo_parent_correctly() {
    weaver = new DependencyInjectingVisitor();
    File repositoryRoot = new File("src/test/resources/poms/fake_repo_root");
    File pom = new File(repositoryRoot, "pom.xml");
    FileWeavingContext ctx = mock(FileWeavingContext.class);
    ChangedFile changedFile =
        ChangedFile.createDefault(
            "src/test/resources/poms/fake_repo_root/code/Foo.java",
            "/tmp/fixed_path",
            Weave.from(5, "some-code", DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT));
    WeavingResult result =
        weaver.visitRepositoryFile(repositoryRoot, pom, ctx, Set.of(changedFile));

    Set<ChangedFile> changedFiles = result.changedFiles();
    assertThat(changedFiles.size(), equalTo(1));
    ChangedFile changedPom = changedFiles.iterator().next();
    assertThat(
        changedPom.originalFilePath(), endsWith("src/test/resources/poms/fake_repo_root/pom.xml"));
    assertThat(changedPom.weaves(), hasItems(Weave.from(1, pomInjectionRuleId)));
  }

  @Test
  void it_scans_to_repo_parent_but_doesnt_inject_when_unnecessary() {
    weaver = new DependencyInjectingVisitor();
    File repositoryRoot = new File("src/test/resources/poms/fake_repo_root");
    File pom = new File(repositoryRoot, "pom.xml");
    FileWeavingContext ctx = mock(FileWeavingContext.class);
    ChangedFile changedFile =
        ChangedFile.createDefault(
            "src/test/resources/poms/fake_repo_root/code/Foo.java",
            "/tmp/fixed_path",
            Weave.from(5, "some-code"));
    WeavingResult result =
        weaver.visitRepositoryFile(repositoryRoot, pom, ctx, Set.of(changedFile));
    Set<ChangedFile> changedFiles = result.changedFiles();
    assertThat(changedFiles.size(), equalTo(0));
  }

  @Test
  void it_doesnt_inject_when_unnecessary() throws Exception {
    var pom = new File("src/test/resources/poms/pom-alreadypresent.xml");
    weaver = new DependencyInjectingVisitor();
    var changedFile =
        weaver.transformPomIfNeeded(pom, Set.of(DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT));
    assertThat(changedFile, is(nullValue()));
  }

  @Test
  void it_injects_our_dependency_using_xpp3() throws Exception {
    var pom = new File("src/test/resources/poms/pom-basic.xml");
    weaver = new DependencyInjectingVisitor(new MavenXpp3RewriterStrategy());
    var changedFile =
        weaver.transformPomIfNeeded(pom, Set.of(DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT));
    assertThat(changedFile, is(not(nullValue())));
    assertThat(changedFile.originalFilePath().endsWith("pom-basic.xml"), is(true));
    assertOnePomWeaveAtLine(changedFile, 1);
    assertHasDependencies(
        changedFile, slf4jDependency, DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
  }

  @Test
  void it_injects_our_dependency_and_owasp_using_xpp3() throws Exception {
    var pom = new File("src/test/resources/poms/pom-basic.xml");
    weaver = new DependencyInjectingVisitor(new MavenXpp3RewriterStrategy());
    var changedFile =
        weaver.transformPomIfNeeded(
            pom,
            Set.of(
                DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT,
                DependencyGAV.OWASP_XSS_JAVA_ENCODER));
    assertThat(changedFile, is(not(nullValue())));
    assertThat(changedFile.originalFilePath().endsWith("pom-basic.xml"), is(true));
    assertOnePomWeaveAtLine(changedFile, 1);
    assertHasDependencies(
        changedFile,
        slf4jDependency,
        DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT,
        DependencyGAV.OWASP_XSS_JAVA_ENCODER);
  }

  @Disabled("XSLT doesn't work yet")
  @Test
  void it_injects_dependency_using_xslt() throws Exception {
    var pom = new File("src/test/resources/poms/pom-basic.xml");
    weaver = new DependencyInjectingVisitor(new XslTransformingStrategy());
    var changedFile =
        weaver.transformPomIfNeeded(pom, Set.of(DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT));
    assertThat(changedFile, is(not(nullValue())));
    assertThat(changedFile.originalFilePath().endsWith("pom-basic.xml"), is(true));
    assertOnePomWeaveAtLine(changedFile, 1);
    assertHasDependencies(
        changedFile, slf4jDependency, DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
  }

  @Test
  void it_injects_dependency_when_none_present_using_xpp3() throws Exception {
    var pom = new File("src/test/resources/poms/pom-nodependencies.xml");
    weaver = new DependencyInjectingVisitor(new MavenXpp3RewriterStrategy());
    var changedFile =
        weaver.transformPomIfNeeded(pom, Set.of(DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT));
    assertThat(changedFile, is(not(nullValue())));
    assertThat(changedFile.originalFilePath().endsWith("pom-nodependencies.xml"), is(true));
    assertOnePomWeaveAtLine(changedFile, 1);
    assertHasOurDependencyAlone(changedFile);
  }

  @Test
  void it_injects_dependency_when_none_present_using_xslt() throws Exception {
    var pom = new File("src/test/resources/poms/pom-nodependencies.xml");
    weaver = new DependencyInjectingVisitor(new XslTransformingStrategy());
    var changedFile =
        weaver.transformPomIfNeeded(pom, Set.of(DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT));
    assertThat(changedFile, is(not(nullValue())));
    assertThat(changedFile.originalFilePath().endsWith("pom-nodependencies.xml"), is(true));
    assertOnePomWeaveAtLine(changedFile, 1);
    assertHasOurDependencyAlone(changedFile);
  }

  private void assertOnePomWeaveAtLine(final ChangedFile changedFile, int lineNumber) {
    var weaves = changedFile.weaves();
    assertThat(weaves.size(), is(1));
    var weave = weaves.iterator().next();
    assertThat(weave.lineNumber(), is(lineNumber));
    assertThat(weave.changeCode(), Matchers.is(DependencyInjectingVisitor.pomInjectionRuleId));
  }

  private void assertHasOurDependencyAlone(final ChangedFile changedFile) throws Exception {
    Model model = toMavenModel(changedFile);

    assertThat(model.getDependencies(), hasSize(1));

    var dependencyFound = model.getDependencies().get(0);
    assertThat(dependencyFound.getGroupId(), equalTo(projectGroup));
    assertThat(dependencyFound.getArtifactId(), equalTo(projectArtifactId));
    assertThat(dependencyFound.getVersion(), equalTo(projectVersion));
  }

  private Model toMavenModel(final ChangedFile changedFile)
      throws IOException, XmlPullParserException {
    var reader = new MavenXpp3Reader();
    var modifiedFile = changedFile.modifiedFile();
    return reader.read(new FileInputStream(modifiedFile));
  }

  private void assertHasDependencies(
      final ChangedFile changedFile, final DependencyGAV... dependencies)
      throws XmlPullParserException, IOException {
    Model model = toMavenModel(changedFile);

    assertThat(model.getDependencies(), hasSize(dependencies.length));

    for (DependencyGAV expectedDependency : dependencies) {
      assertThat(
          model.getDependencies().stream()
              .anyMatch(
                  actualDependency ->
                      actualDependency.getGroupId().equals(expectedDependency.group())
                          && actualDependency.getArtifactId().equals(expectedDependency.artifact())
                          && actualDependency.getVersion().equals(expectedDependency.version())),
          is(true));
    }
  }

  private static final DependencyGAV slf4jDependency =
      DependencyGAV.createDefault("org.slf4j", "slf4j-api", "1.7.25");
}

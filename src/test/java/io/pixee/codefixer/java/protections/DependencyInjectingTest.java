package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.DependencyInjectingVisitor.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import io.pixee.codefixer.java.ChangedFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

final class DependencyInjectingTest {

  private DependencyInjectingVisitor weaver;

  @Test
  void it_doesnt_inject_when_unnecessary() throws Exception {
    var pom = new File("src/test/resources/poms/pom-alreadypresent.xml");
    weaver = new DependencyInjectingVisitor();
    var changedFile = weaver.transformPomIfNeeded(pom);
    assertThat(changedFile, is(nullValue()));
  }

  @Test
  void it_injects_dependency_using_xpp3() throws Exception {
    var pom = new File("src/test/resources/poms/pom-basic.xml");
    weaver = new DependencyInjectingVisitor(new MavenXpp3RewriterStrategy());
    var changedFile = weaver.transformPomIfNeeded(pom);
    assertThat(changedFile, is(not(nullValue())));
    assertThat(changedFile.originalFilePath().endsWith("pom-basic.xml"), is(true));
    assertOnePomWeaveAtLine(changedFile, 1);
    assertHasBasicAndOurDependency(changedFile);
  }

  @Disabled("XSLT doesn't work yet")
  @Test
  void it_injects_dependency_using_xslt() throws Exception {
    var pom = new File("src/test/resources/poms/pom-basic.xml");
    weaver = new DependencyInjectingVisitor(new XslTransformingStrategy());
    var changedFile = weaver.transformPomIfNeeded(pom);
    assertThat(changedFile, is(not(nullValue())));
    assertThat(changedFile.originalFilePath().endsWith("pom-basic.xml"), is(true));
    assertOnePomWeaveAtLine(changedFile, 1);
    assertHasBasicAndOurDependency(changedFile);
  }

  @Test
  void it_injects_dependency_when_none_present_using_xpp3() throws Exception {
    var pom = new File("src/test/resources/poms/pom-nodependencies.xml");
    weaver = new DependencyInjectingVisitor(new MavenXpp3RewriterStrategy());
    var changedFile = weaver.transformPomIfNeeded(pom);
    assertThat(changedFile, is(not(nullValue())));
    assertThat(changedFile.originalFilePath().endsWith("pom-nodependencies.xml"), is(true));
    assertOnePomWeaveAtLine(changedFile, 1);
    assertHasOurDependencyAlone(changedFile);
  }

  @Test
  void it_injects_dependency_when_none_present_using_xslt() throws Exception {
    var pom = new File("src/test/resources/poms/pom-nodependencies.xml");
    weaver = new DependencyInjectingVisitor(new XslTransformingStrategy());
    var changedFile = weaver.transformPomIfNeeded(pom);
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

  private void assertHasBasicAndOurDependency(final ChangedFile changedFile)
      throws XmlPullParserException, IOException {
    Model model = toMavenModel(changedFile);

    assertThat(model.getDependencies(), hasSize(2));

    var slf4jDependency = model.getDependencies().get(0);
    assertThat(slf4jDependency.getGroupId(), equalTo("org.slf4j"));
    assertThat(slf4jDependency.getArtifactId(), equalTo("slf4j-api"));
    assertThat(slf4jDependency.getVersion(), equalTo("1.7.25"));

    var ourDependency = model.getDependencies().get(1);
    assertThat(ourDependency.getGroupId(), equalTo(projectGroup));
    assertThat(ourDependency.getArtifactId(), equalTo(projectArtifactId));
    assertThat(ourDependency.getVersion(), equalTo(projectVersion));
  }
}

package io.openpixee.java.protections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import io.codemodder.ChangedFile;
import io.codemodder.DependencyGAV;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.openpixee.maven.operator.Dependency;
import io.openpixee.maven.operator.POMOperator;
import io.openpixee.maven.operator.ProjectModel;
import io.openpixee.maven.operator.ProjectModelFactory;
import io.openpixee.maven.operator.QueryType;
import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DependencyGAVInjectingTest {
  private DependencyInjectingVisitor weaver;

  @BeforeEach
  void setup() {
    weaver = new DependencyInjectingVisitor();
  }

  @Test
  void it_injects_our_dependency() {
    var pom = new File("src/test/resources/poms/fake_repo_root/pom.xml");
    var changedFile =
        weaver.visitRepositoryFile(
            pom.getParentFile(),
            pom,
            mock(FileWeavingContext.class),
            Set.of(
                ChangedFile.createDefault(
                    "src/test/resources/poms/fake_repo_root/code/Foo.java",
                    "/tmp/fixed_path",
                    Weave.from(5, "some-code", DependencyGAV.JAVA_SECURITY_TOOLKIT))));

    assertHasDependencies(
        new File(changedFile.changedFiles().iterator().next().modifiedFile()),
        slf4jDependency,
        DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }

  @Test
  void it_preserves_original_formatting() throws Exception {
    var pom = new File("src/test/resources/poms/fake_repo_root/pom.xml");
    var changedFile =
        weaver.visitRepositoryFile(
            pom.getParentFile(),
            pom,
            mock(FileWeavingContext.class),
            Set.of(
                ChangedFile.createDefault(
                    "src/test/resources/poms/fake_repo_root/code/Foo.java",
                    "/tmp/fixed_path",
                    Weave.from(5, "some-code", DependencyGAV.JAVA_SECURITY_TOOLKIT))));

    File modifiedPomFile = new File(changedFile.changedFiles().iterator().next().modifiedFile());

    assertHasDependencies(modifiedPomFile, slf4jDependency, DependencyGAV.JAVA_SECURITY_TOOLKIT);

    assertHasMatches(modifiedPomFile, "<distributionManagement></distributionManagement>", 1);

    assertHasMatches(modifiedPomFile, "<email/>", 1);

    assertHasMatches(modifiedPomFile, "<email></email>", 1);

    assertHasMatches(modifiedPomFile, "<email />", 1);
  }

  private void assertHasMatches(File sourceFile, String textPattern, int expectedNoOcurrences)
      throws Exception {
    String content = FileUtils.readFileToString(sourceFile);

    Pattern patternToFind = Pattern.compile(Pattern.quote(textPattern));

    Matcher matcher = patternToFind.matcher(content);
    int noOcurrences = 0;

    while (matcher.find()) noOcurrences++;

    String expectedMessage =
        String.format(
            "Expected to find %d ocurrences of string '%s' in file %s. Found %d times instead",
            expectedNoOcurrences, textPattern, sourceFile.getAbsolutePath(), noOcurrences);

    assertEquals(expectedNoOcurrences, noOcurrences, expectedMessage);
  }

  @Test
  void it_injects_our_dependency_and_owasp() {
    var pom = new File("src/test/resources/poms/fake_repo_root/pom.xml");
    var changedFile =
        weaver.visitRepositoryFile(
            pom.getParentFile(),
            pom,
            mock(FileWeavingContext.class),
            Set.of(
                ChangedFile.createDefault(
                    "src/test/resources/poms/fake_repo_root/code/Foo.java",
                    "/tmp/fixed_path",
                    Weave.from(5, "some-code", DependencyGAV.JAVA_SECURITY_TOOLKIT)),
                ChangedFile.createDefault(
                    "src/test/resources/poms/fake_repo_root/code/Foo.java",
                    "/tmp/fixed_path",
                    Weave.from(5, "some-code", DependencyGAV.OWASP_XSS_JAVA_ENCODER))));

    assertHasDependencies(
        new File(changedFile.changedFiles().iterator().next().modifiedFile()),
        slf4jDependency,
        DependencyGAV.JAVA_SECURITY_TOOLKIT,
        DependencyGAV.OWASP_XSS_JAVA_ENCODER);
  }

  private void assertHasDependencies(File pom, DependencyGAV... dependencies) {
    ProjectModel projectModel =
        ProjectModelFactory.load(pom).withQueryType(QueryType.UNSAFE).build();

    Collection<Dependency> foundDependencies = POMOperator.queryDependency(projectModel);

    for (DependencyGAV dependency : dependencies) {
      Optional<Dependency> foundDependency =
          foundDependencies.stream()
              .filter(
                  d ->
                      d.getGroupId().equals(dependency.group())
                          && d.getArtifactId().equals(dependency.artifact())
                          && d.getVersion().equals(dependency.version()))
              .findFirst();

      assertTrue(foundDependency.isPresent(), "Dependency " + dependency + " is present");
    }
  }

  private static final DependencyGAV slf4jDependency =
      DependencyGAV.createDefault("org.slf4j", "slf4j-api", "1.7.25");
}

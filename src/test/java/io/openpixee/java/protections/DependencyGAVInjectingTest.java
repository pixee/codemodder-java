package io.openpixee.java.protections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.openpixee.java.ChangedFile;
import io.openpixee.java.DependencyGAV;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.Weave;
import io.openpixee.maven.operator.Dependency;
import io.openpixee.maven.operator.POMOperator;
import io.openpixee.maven.operator.ProjectModel;
import io.openpixee.maven.operator.ProjectModelFactory;
import io.openpixee.maven.operator.QueryType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
                    Weave.from(5, "some-code", DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT))));

    assertHasDependencies(
        new File(changedFile.changedFiles().iterator().next().modifiedFile()),
        slf4jDependency,
        DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
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
                    Weave.from(5, "some-code", DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT)),
                ChangedFile.createDefault(
                    "src/test/resources/poms/fake_repo_root/code/Foo.java",
                    "/tmp/fixed_path",
                    Weave.from(5, "some-code", DependencyGAV.OWASP_XSS_JAVA_ENCODER))));

    assertHasDependencies(
        new File(changedFile.changedFiles().iterator().next().modifiedFile()),
        slf4jDependency,
        DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT,
        DependencyGAV.OWASP_XSS_JAVA_ENCODER);
  }

  private void assertHasDependencies(File pom, DependencyGAV... dependencies) {
    ProjectModel projectModel =
        ProjectModelFactory.load(pom).withQueryType(QueryType.UNSAFE).build();

    Collection<Dependency> foundDependencies = POMOperator.queryDependency(projectModel, List.of());

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

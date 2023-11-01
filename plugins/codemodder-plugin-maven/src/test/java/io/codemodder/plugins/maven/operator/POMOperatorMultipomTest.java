package io.codemodder.plugins.maven.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.collections.MapsKt;
import kotlin.jvm.internal.Intrinsics;
import org.junit.jupiter.api.Test;

final class POMOperatorMultipomTest extends AbstractTestBase {

  /**
   * Test case for a scenario where a parent and child project have missing packaging information.
   * This test checks if an exception of type WrongDependencyTypeException is thrown.
   */
  @Test
  void testWithParentAndChildMissingPackaging() {
    assertThatThrownBy(
            () -> {
              File parentResource = getResource("parent-and-child-parent-broken.xml");

              List<POMDocument> parentPomFiles =
                  Arrays.asList(POMDocumentFactory.load(parentResource));

              ProjectModelFactory parentPom =
                  ProjectModelFactory.load(parentResource).withParentPomFiles(parentPomFiles);

              gwt(
                  "parent-and-child",
                  parentPom.withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3")));
            })
        .isInstanceOf(WrongDependencyTypeException.class);
  }

  /**
   * Test case for a scenario where a parent-child project has an incorrect dependency type. This
   * test checks if an exception of type WrongDependencyTypeException is thrown.
   */
  @Test
  void testWithParentAndChildWrongType() {
    assertThatThrownBy(
            () -> {
              File parentResource = getResource("parent-and-child-child-broken.xml");

              POMDocument parentPomFile =
                  POMDocumentFactory.load(getResource("parent-and-child-parent.xml"));

              List<POMDocument> parentPomFiles = Arrays.asList(parentPomFile);

              ProjectModelFactory parentPom =
                  ProjectModelFactory.load(parentResource).withParentPomFiles(parentPomFiles);

              gwt(
                  "parent-and-child-wrong-type",
                  parentPom.withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3")));
            })
        .isInstanceOf(WrongDependencyTypeException.class);
  }

  /**
   * Test case for a scenario with multiple POMs where the child POM does not have a version
   * property, and properties are not used. This test validates dependency resolution and checks if
   * the resulting POM document (pom-multiple-pom-basic-no-version-property-result.xml) is marked as
   * dirty.
   */
  @Test
  void testWithMultiplePomsBasicNoVersionProperty() throws Exception {
    File parentPomFile = getResource("sample-parent/pom.xml");

    ProjectModelFactory projectModelFactory =
        ProjectModelFactory.load(getResource("sample-child-with-relativepath.xml"))
            .withParentPomFiles(Arrays.asList(POMDocumentFactory.load(parentPomFile)))
            .withUseProperties(false);

    ProjectModel result =
        gwt(
            "multiple-pom-basic-no-version-property",
            projectModelFactory.withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3")));

    validateDepsFrom(result);

    assertThat(result.allPomFiles().size()).isEqualTo(2);
    assertThat(result.allPomFiles().stream().allMatch(POMDocument::isDirty)).isTrue();
  }

  /**
   * Test case for a scenario with multiple POMs where the child POM has a version property, and
   * properties are used. This test validates dependency resolution, property usage, and checks if
   * the resulting POM document (pom-multiple-pom-basic-with-version-property-result.xml) is marked
   * as dirty.
   */
  @Test
  void testWithMultiplePomsBasicWithVersionProperty() throws Exception {
    File parentPomFile = getResource("sample-parent/pom.xml");

    File sampleChild = getResource("sample-child-with-relativepath.xml");

    ProjectModelFactory parentPom =
        ProjectModelFactory.load(sampleChild)
            .withParentPomFiles(Arrays.asList(POMDocumentFactory.load(parentPomFile)))
            .withUseProperties(true);

    ProjectModel result =
        gwt(
            "multiple-pom-basic-with-version-property",
            parentPom.withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3")));

    validateDepsFrom(result);

    assertThat(result.allPomFiles().size()).isEqualTo(2);
    assertThat(result.allPomFiles().stream().allMatch(POMDocument::isDirty)).isTrue();

    String parentPomString =
        new String(result.getParentPomFiles().get(0).getResultPomBytes(), StandardCharsets.UTF_8);
    String pomString = new String(result.getPomFile().getResultPomBytes(), StandardCharsets.UTF_8);

    String version = result.getDependency().getVersion();

    assertThat(parentPomString).contains("versions.dom4j>" + version);
    assertThat(pomString).doesNotContain(version);
  }

  void validateDepsFrom(ProjectModel context) throws Exception {
    Map<POMDocument, File> resultFiles = copyFiles(context);

    resultFiles
        .entrySet()
        .forEach(
            entry -> {
              System.err.println("from " + entry.getKey().getPomPath() + " -> " + entry.getValue());
            });

    File pomFile = resultFiles.entrySet().iterator().next().getValue();

    ProjectModelFactory factory = ProjectModelFactory.load(pomFile);
    factory.withQueryType(QueryType.UNSAFE);
    ProjectModel projectModel = factory.build();

    Collection<Dependency> dependencies = POMOperator.queryDependency(projectModel);

    Optional<Dependency> optionalDependency =
        dependencies.stream()
            .filter(
                dependency -> dependency.matchesWithoutConsideringVersion(context.getDependency()))
            .findFirst();

    assertThat(optionalDependency).isNotEmpty();
  }

  Map<POMDocument, File> copyFiles(ProjectModel context) throws IOException, URISyntaxException {
    File commonPath = new File(context.getPomFile().getPomPath().toURI()).getCanonicalFile();

    for (POMDocument p : context.getParentPomFiles()) {
      String pCanonicalPath = new File(p.getPomPath().toURI()).getCanonicalPath();
      commonPath =
          new File(commonPath.getCanonicalPath())
              .toPath()
              .relativize(new File(pCanonicalPath).toPath())
              .toFile();
    }

    int commonPathLen = commonPath.getParent().length();

    File tmpOutputDir = Files.createTempDir();

    return buildMap(context, commonPathLen, tmpOutputDir);
  }

  // Using original kotlin code decompiled version
  Map<POMDocument, File> buildMap(ProjectModel context, int commonPathLen, File tmpOutputDir)
      throws URISyntaxException, IOException {
    List allPomFiles = context.allPomFiles();
    Iterable allPomFilesIterable = (Iterable) allPomFiles;
    Collection allPomFilesCollections = (Collection) new ArrayList<>();

    Iterator var10 = allPomFilesIterable.iterator();

    while (var10.hasNext()) {
      Object item$iv$iv = var10.next();
      POMDocument p = (POMDocument) item$iv$iv;
      Intrinsics.checkNotNullExpressionValue(p, "p");
      URL var10002 = p.getPomPath();
      Intrinsics.checkNotNull(var10002);
      File pAsFile = new File(var10002.toURI());
      String var21 = pAsFile.getCanonicalPath();
      Intrinsics.checkNotNullExpressionValue(var21, "pAsFile.canonicalPath");
      String var15 = var21;
      int var16 = 1 + commonPathLen;
      var21 = var15.substring(var16);
      Intrinsics.checkNotNullExpressionValue(
          var21, "this as java.lang.String).substring(startIndex)");
      String relPath = var21;
      File targetPath = new File(tmpOutputDir, relPath);
      if (!targetPath.getParentFile().exists()) {
        targetPath.getParentFile().mkdirs();
      }

      (new FileOutputStream(targetPath)).write(p.getResultPomBytes());
      Pair var19 = TuplesKt.to(p, targetPath);
      allPomFilesCollections.add(var19);
    }

    Map result = MapsKt.toMap((Iterable) ((List) allPomFilesCollections));
    return result;
  }
}

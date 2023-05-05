package io.codemodder.testutils;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import io.codemodder.*;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.CachingJavaParser;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The basic tests for codemods. */
public interface CodemodTestMixin {

  @Test
  default void it_verifies_codemod(@TempDir final Path tmpDir) throws IOException {
    Metadata metadata = getClass().getAnnotation(Metadata.class);
    if (metadata == null) {
      throw new IllegalArgumentException("CodemodTest must be annotated with @Metadata");
    }

    Class<? extends Changer> codemod = metadata.codemodType();
    Path testResourceDir = Path.of(metadata.testResourceDir());

    List<DependencyGAV> dependencies =
        Arrays.stream(metadata.dependencies())
            .map(
                dependency -> {
                  String[] tokens = dependency.split(":");
                  return DependencyGAV.createDefault(tokens[0], tokens[1], tokens[2]);
                })
            .collect(Collectors.toUnmodifiableList());

    Path testDir = Path.of("src/test/resources/" + testResourceDir);
    verifyCodemod(codemod, tmpDir, testDir, dependencies);
  }

  private void verifyCodemod(
      final Class<? extends Changer> codemodType,
      final Path tmpDir,
      final Path testResourceDir,
      final List<DependencyGAV> dependenciesExpected)
      throws IOException {

    // create a copy of the test file in the temp directory to serve as our "repository"
    Path before = testResourceDir.resolve("Test.java.before");
    Path after = testResourceDir.resolve("Test.java.after");
    Path pathToJavaFile = tmpDir.resolve("Test.java");
    Files.copy(before, pathToJavaFile);

    // Check for any sarif files and build the RuleSarif map
    List<File> allSarifs = new ArrayList<>();
    Files.newDirectoryStream(testResourceDir, "*.sarif")
        .iterator()
        .forEachRemaining(p -> allSarifs.add(p.toFile()));

    Map<String, List<RuleSarif>> map = SarifParser.create().parseIntoMap(allSarifs, tmpDir);

    // run the codemod
    CodemodLoader loader = new CodemodLoader(List.of(codemodType), tmpDir, map);

    List<CodemodIdPair> codemods = loader.getCodemods();
    assertThat(codemods.size(), equalTo(1));
    CodemodIdPair codemod = codemods.get(0);
    JavaParserFactory factory = JavaParserFactory.newFactory();
    SourceDirectory dir = SourceDirectory.createDefault(tmpDir, List.of(pathToJavaFile.toString()));
    CodemodExecutor executor =
        CodemodExecutor.from(
            tmpDir,
            IncludesExcludes.any(),
            codemod,
            List.of(),
            CachingJavaParser.from(factory.create(List.of(dir))),
            EncodingDetector.create());
    CodeTFResult result = executor.execute(List.of(pathToJavaFile));
    List<CodeTFChangesetEntry> changeset = result.getChangeset();

    // make sure the file is transformed to the expected output
    String expectedCode = Files.readString(after);
    String transformedJavaCode = Files.readString(pathToJavaFile);
    assertThat(transformedJavaCode, equalTo(expectedCode));
    assertThat(changeset.size(), is(1));
    CodeTFChangesetEntry entry = changeset.get(0);
    assertThat(entry.getChanges().isEmpty(), is(false));

    // make sure the dependencies are added

    assertThat(dependenciesExpected, hasItems(dependenciesExpected.toArray(new DependencyGAV[0])));

    // re-run the transformation again and make sure no changes are made
    CodeTFResult result2 = executor.execute(List.of(pathToJavaFile));
    List<CodeTFChangesetEntry> changeset2 = result2.getChangeset();
    assertThat(changeset2.size(), is(0));
    String transformedAgainJavaCode = Files.readString(pathToJavaFile);
    assertThat(transformedAgainJavaCode, equalTo(expectedCode));
  }
}

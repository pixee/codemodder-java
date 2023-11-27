package io.codemodder.codemods;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.javaparser.JavaParser;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.JavaParserFacade;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class VerbTamperingCodemodTest {

  private static String safeXmlAfterCodemod;

  @BeforeAll
  static void setup() throws IOException {
    safeXmlAfterCodemod =
        Files.readString(Path.of("src/test/resources/verb-tampering/web-after-codemod.xml"));
  }

  @ParameterizedTest
  @MethodSource("cases")
  void it_removes_verb_tampering(
      final String webXmlDir,
      final boolean expectChange,
      final Set<Integer> expectedAffectedLines,
      final @TempDir Path tmpDir)
      throws IOException {
    String dir = "src/test/resources/verb-tampering/" + webXmlDir;
    copyDir(Path.of(dir), tmpDir);

    Path webxml = tmpDir.resolve("web.xml");

    CodemodLoader loader =
        new CodemodLoader(
            List.of(VerbTamperingCodemod.class),
            CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
            tmpDir,
            List.of("**"),
            List.of(),
            List.of(webxml),
            Map.of(),
            List.of(),
            null);

    CodemodExecutor executor =
        CodemodExecutorFactory.from(
            tmpDir,
            IncludesExcludes.any(),
            loader.getCodemods().get(0),
            List.of(),
            List.of(),
            FileCache.createDefault(),
            JavaParserFacade.from(JavaParser::new),
            EncodingDetector.create());
    CodeTFResult result = executor.execute(List.of(webxml));
    assertThat(result.getChangeset().isEmpty(), is(!expectChange));

    if (expectChange) {
      CodeTFChangesetEntry changedFile = result.getChangeset().get(0);
      String modifiedFile = Files.readString(webxml);

      List<Integer> linesAffected =
          changedFile.getChanges().stream()
              .map(CodeTFChange::getLineNumber)
              .collect(Collectors.toUnmodifiableList());
      assertThat(linesAffected, hasItems(expectedAffectedLines.toArray(new Integer[0])));
      assertThat(modifiedFile, equalTo(safeXmlAfterCodemod));
    }
  }

  private static void copyDir(Path src, Path dest) throws IOException {
    String srcPath = src.toString();
    String destPath = dest.toString();
    Files.walk(Paths.get(srcPath))
        .forEach(
            a -> {
              Path b = Paths.get(destPath, a.toString().substring(srcPath.length()));
              if (!a.toString().equals(srcPath)) {
                try {
                  Files.copy(a, b, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
            });
  }

  static Stream<Arguments> cases() {
    return Stream.of(
        Arguments.of("on_own_line", true, Set.of(5, 6)),
        Arguments.of("sharing_line", true, Set.of(5)),
        Arguments.of("sharing_line_with_others", true, Set.of(4)),
        Arguments.of("safe", false, Set.of()));
  }
}

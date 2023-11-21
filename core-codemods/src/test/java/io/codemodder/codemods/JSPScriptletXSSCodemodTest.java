package io.codemodder.codemods;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.javaparser.JavaParser;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFChange;
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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JSPScriptletXSSCodemodTest {

  @ParameterizedTest
  @MethodSource("cases")
  void it_fixes_jsp(
      final String jspDir,
      final boolean expectChange,
      final Set<Integer> expectedAffectedLines,
      final @TempDir Path tmpDir)
      throws IOException {
    String dir = "src/test/resources/encode-jsp-scriptlet/" + jspDir;
    copyDir(Path.of(dir), tmpDir);

    Path beforeJsp = tmpDir.resolve("test.jsp.before");
    Path jsp = tmpDir.resolve("test.jsp");
    Files.copy(beforeJsp, jsp);

    CodemodLoader codemodInvoker =
        new CodemodLoader(
            List.of(JSPScriptletXSSCodemod.class),
            CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
            tmpDir,
            List.of("**"),
            List.of(),
            List.of(jsp),
            Map.of(),
            List.of(),
            null);
    CodemodIdPair codemod = codemodInvoker.getCodemods().get(0);
    CodemodExecutor executor =
        CodemodExecutorFactory.from(
            tmpDir,
            IncludesExcludes.any(),
            codemod,
            List.of(),
            List.of(),
            FileCache.createDefault(),
            JavaParserFacade.from(JavaParser::new),
            EncodingDetector.create());
    CodeTFResult result = executor.execute(List.of(jsp));

    assertThat(result.getFailedFiles().isEmpty(), is(true));

    if (expectChange) {
      String modifiedFile = Files.readString(jsp);
      List<Integer> linesAffected =
          result.getChangeset().get(0).getChanges().stream()
              .map(CodeTFChange::getLineNumber)
              .collect(Collectors.toUnmodifiableList());
      assertThat(linesAffected, hasItems(expectedAffectedLines.toArray(new Integer[0])));
      String expectedAfterContents = Files.readString(tmpDir.resolve("test.jsp.after"));
      assertThat(modifiedFile, equalTo(expectedAfterContents));
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
        Arguments.of("multiple_lines", true, Set.of(3, 5, 8, 10)),
        Arguments.of("simple", true, Set.of(1)),
        Arguments.of("no_changes", false, Set.of()));
  }
}

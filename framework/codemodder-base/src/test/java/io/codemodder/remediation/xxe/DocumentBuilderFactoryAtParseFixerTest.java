package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class DocumentBuilderFactoryAtParseFixerTest {

  private Remediator<Object> fixer;

  private static final String unfixableBecauseDocumentBuilderCameFromOutside =
      """
            public class MyCode {
                  public void foo(DocumentBuilder db) {
                        boolean success;
                        try
                        {
                            db.parse(new InputSource(sr));
                            success = true;
                        } catch (FileNotFoundException e) {
                            success = false;
                            logError(e);
                        }
                  }
                }
            """;

  private static final String unfixableBecauseDocumentBuilderTypeIsntLocal =
      """
            public class MyCode {
                  public void foo() {
                        boolean success;
                        try
                        {
                            getDb().parse(new InputSource(sr));
                            success = true;
                        } catch (FileNotFoundException e) {
                            success = false;
                            logError(e);
                        }
                  }
                  private DocumentBuilder getDb() {
                     return DocumentBuilderFactory.newInstance().newDocumentBuilder();
                  }
                }
            """;

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        Arguments.of(
            unfixableBecauseDocumentBuilderCameFromOutside,
            6,
            19,
            "DocumentBuilder came from outside the method scope"),
        Arguments.of(
            unfixableBecauseDocumentBuilderTypeIsntLocal,
            6,
            null,
            RemediationMessages.noNodesAtThatLocation));
  }

  @BeforeEach
  void setup() {
    fixer =
        new SearcherStrategyRemediator.Builder<>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<Object>()
                    .withMatcher(DocumentBuilderFactoryAtParseFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new DocumentBuilderFactoryAtParseFixStrategy())
            .build();
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_doesnt_fix(final String code, final int line, final Integer column, final String reason) {
    CompilationUnit cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var result =
        fixer.remediateAll(
            cu,
            "path",
            new DetectorRule("", "", ""),
            List.of(new Object()),
            o -> "id",
            o -> line,
            o -> Optional.empty(),
            o -> Optional.ofNullable(column));
    assertThat(result.unfixedFindings().isEmpty()).isFalse();
    assertThat(result.unfixedFindings().get(0).getReason()).isEqualTo(reason);
  }

  @Test
  void it_fixes_dbf_at_parse_call() {
    String vulnerableCode =
        """
                public class MyCode {
                  public void foo() {
                        XMLReader parser = null;
                        DocumentBuilderFactory dbf = null;
                        StringReader sr = null;
                        boolean success;
                        try
                        {
                            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                            DocumentBuilder db = dbf.newDocumentBuilder();
                            db.parse(new InputSource(sr));
                            success = true;
                        } catch (FileNotFoundException e){
                            success = false;
                            logError(e);
                        }
                  }
                }
                """;

    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);
    var result =
        fixer.remediateAll(
            cu,
            "path",
            new DetectorRule("", "", ""),
            List.of(new Object()),
            o -> "id",
            o -> 11,
            o -> Optional.empty(),
            o -> Optional.ofNullable(16));
    assertThat(result.changes().isEmpty()).isFalse();

    String fixedCode =
        """
                            public class MyCode {
                              public void foo() {
                                    XMLReader parser = null;
                                    DocumentBuilderFactory dbf = null;
                                    StringReader sr = null;
                                    boolean success;
                                    try
                                    {
                                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                                        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                                        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                                        DocumentBuilder db = dbf.newDocumentBuilder();
                                        db.parse(new InputSource(sr));
                                        success = true;
                                    } catch (FileNotFoundException e){
                                        success = false;
                                        logError(e);
                                    }
                              }
                            }
                            """;

    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualToIgnoringCase(fixedCode);
  }
}

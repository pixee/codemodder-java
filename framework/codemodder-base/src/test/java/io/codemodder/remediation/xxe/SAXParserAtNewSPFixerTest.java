package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.SearcherStrategyRemediator;
import io.codemodder.remediation.WithoutScopePositionMatcher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class SAXParserAtNewSPFixerTest {

  private Remediator<Object> fixer;

  @BeforeEach
  void setup() {
    fixer =
        new SearcherStrategyRemediator.Builder<>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<Object>()
                    .withMatcher(SAXParserAtNewSPFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new SAXParserAtNewSPFixStrategy())
            .build();
  }

  @Test
  void it_fixes_sax_parser_at_new_sp_call() {
    String vulnerableCode =
        """
                public class MyCode {
                  public void foo() {
                        try
                        {
                            SAXParserFactory factory = getFactory();
                            SAXParser parser = factory.newSAXParser();
                            parser.parse(new InputSource(sr));
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
            o -> 6,
            o -> Optional.empty(),
            o -> Optional.empty());
    assertThat(result.changes().isEmpty()).isFalse();

    String fixedCode =
        """
                            public class MyCode {
                              public void foo() {
                                    try
                                    {
                                        SAXParserFactory factory = getFactory();
                                        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                                        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                                        SAXParser parser = factory.newSAXParser();
                                        parser.parse(new InputSource(sr));
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

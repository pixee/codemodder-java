package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.SearcherStrategyRemediator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class TransformerFactoryAtCreationFixerTest {

  private Remediator<Object> fixer;

  @BeforeEach
  void setup() {
    fixer =
        new SearcherStrategyRemediator.Builder<>()
            .withMatchAndFixStrategy(new TransformerFactoryAtCreationFixStrategy())
            .build();
  }

  @Test
  void it_fixes_transformer() {
    String vulnerableCode =
        """
                        public class MyCode {
                          public void foo() {
                            TransformerFactory factory = TransformerFactory.newInstance();
                            factory.newTransformer().transform(new StreamSource(new StringReader(xml)), new StreamResult(new StringWriter()));
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
            o -> 3,
            o -> Optional.empty(),
            o -> Optional.ofNullable(52));
    assertThat(result.changes().isEmpty()).isFalse();

    String fixedCode =
        """
                    import javax.xml.XMLConstants;

                    public class MyCode {
                      public void foo() {
                        TransformerFactory factory = TransformerFactory.newInstance();
                        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                        factory.newTransformer().transform(new StreamSource(new StringReader(xml)), new StreamResult(new StringWriter()));
                      }
                    }
                    """;

    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualToIgnoringCase(fixedCode);
  }
}

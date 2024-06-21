package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class TransformerFactoryAtCreationFixerTest {

  private TransformerFactoryAtCreationFixer fixer;

  @BeforeEach
  void setup() {
    this.fixer = new TransformerFactoryAtCreationFixer();
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
    XXEFixAttempt fixAttempt = fixer.tryFix(3, 52, cu);
    assertThat(fixAttempt.isFixed()).isTrue();
    assertThat(fixAttempt.isResponsibleFixer()).isTrue();
    assertThat(fixAttempt.reasonNotFixed()).isNullOrEmpty();

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

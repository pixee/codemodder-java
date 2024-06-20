package io.codemodder.codemods;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DefaultXXEJavaDOMRemediatorStrategyTest {

  private DefaultXXEJavaDOMRemediatorStrategy remediator;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    this.remediator = new DefaultXXEJavaDOMRemediatorStrategy();
    this.rule = new DetectorRule("xxe", "XXE", null);
  }

  private static class Finding {
    private final String key;
    private final int line;
    private final int column;

    Finding(String key, int line, int column) {
      this.key = key;
      this.line = line;
      this.column = column;
    }

    int getLine() {
      return line;
    }

    String getKey() {
      return key;
    }

    int getColumn() {
      return column;
    }
  }

  @Test
  void it_fixes_transformers() {
    String vulnerableCode =
        """
            public class MyCode {
              public void foo() {
                TransformerFactory factory = TransformerFactory.newInstance();
                factory.newTransformer().transform(new StreamSource(new StringReader(xml)), new StreamResult(new StringWriter()));
              }
            }
            """;
    List<Finding> findings = List.of(new Finding("foo", 3, 52));
    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "foo", rule, findings, Finding::getKey, Finding::getLine, Finding::getColumn);
    assertThat(result.unfixedFindings()).isEmpty();
    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(3);

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

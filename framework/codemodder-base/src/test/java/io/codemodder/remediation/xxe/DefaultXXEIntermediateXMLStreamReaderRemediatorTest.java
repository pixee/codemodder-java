package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DefaultXXEIntermediateXMLStreamReaderRemediatorTest {

  private DefaultXXEIntermediateXMLStreamReaderRemediator remediator;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    remediator = new DefaultXXEIntermediateXMLStreamReaderRemediator();
    rule = new DetectorRule("xxe", "XXE Fixed At XMLStreamReader", null);
  }

  @Test
  void it_remediates_finding() {
    String fixableCode =
        """
                import javax.xml.stream.XMLInputFactory;
                import javax.xml.stream.XMLStreamReader;
                import java.io.StringReader;

                class MessageReader {

                    Message read(String xml) {
                        XMLInputFactory factory = XMLInputFactory.newInstance();
                        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
                        // turn into a Message
                        Message message = convertMessage(reader);
                        return message;
                    }
                }
                """;
    CompilationUnit cu = StaticJavaParser.parse(fixableCode);
    LexicalPreservingPrinter.setup(cu);

    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "foo", rule, List.of(new Object()), f -> "my-id-1", f -> 9, f -> null);

    // confirm code is what's expected
    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode)
        .isEqualToIgnoringWhitespace(
            """
                import javax.xml.stream.XMLInputFactory;
                import javax.xml.stream.XMLStreamReader;
                import java.io.StringReader;

                class MessageReader {
                    Message read(String xml) {
                        XMLInputFactory factory = XMLInputFactory.newInstance();
                        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
                        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
                        // turn into a Message
                        Message message = convertMessage(reader);
                        return message;
                    }
                }
                """);

    // confirm reporting metadata is all correct
    assertThat(result.unfixedFindings()).isEmpty();

    List<CodemodChange> changes = result.changes();
    assertThat(changes).hasSize(1);

    CodemodChange change = changes.get(0);
    assertThat(change.lineNumber()).isEqualTo(9);
    assertThat(change.getDependenciesNeeded()).isEmpty();
    ;
    List<FixedFinding> fixedFindings = change.getFixedFindings();
    assertThat(fixedFindings).hasSize(1);
    FixedFinding fixedFinding = fixedFindings.get(0);
    assertThat(fixedFinding.getId()).isEqualTo("my-id-1");
    assertThat(fixedFinding.getRule()).isEqualTo(rule);
  }
}

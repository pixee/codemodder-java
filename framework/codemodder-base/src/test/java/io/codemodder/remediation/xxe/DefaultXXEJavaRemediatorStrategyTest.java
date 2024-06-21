package io.codemodder.remediation.xxe;

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

final class DefaultXXEJavaRemediatorStrategyTest {

  private DefaultXXEJavaRemediatorStrategy remediator;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    this.remediator = new DefaultXXEJavaRemediatorStrategy();
    this.rule = new DetectorRule("xxe", "XXE", null);
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

    List<XXEFinding> findings = List.of(new XXEFinding("foo", 11, 16));
    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "foo", rule, findings, XXEFinding::key, XXEFinding::line, XXEFinding::column);
    assertThat(result.unfixedFindings()).isEmpty();
    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(11);

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

  @Test
  void it_doesnt_fix_because_unknown_parser() {
    String unfixableBecauseParserIsUnknownAndNobodyClaimsIt =
        """
            public class MyCode {
              public void foo() {
                    SomeOtherXMLThing parser = null;
                    DocumentBuilderFactory dbf = null;
                    StringReader sr = null;
                    boolean success;
                    try
                    {
                        parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
                        parser.setFeature(VALIDATION, true);
                        parser.setErrorHandler(new MyErrorHandler());
                        parser.setProperty(JAXP_SCHEMA_SOURCE, new File(schemaName));
                        sr = new StringReader(str);
                        parser.parse(new InputSource(sr));
                        success = true;
                    } catch (FileNotFoundException e){
                        success = false;
                        logError(e);
                    }
              }
            }
            """;

    List<XXEFinding> findings = List.of(new XXEFinding("foo", 14, 19));
    CompilationUnit cu = StaticJavaParser.parse(unfixableBecauseParserIsUnknownAndNobodyClaimsIt);
    LexicalPreservingPrinter.setup(cu);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "foo", rule, findings, XXEFinding::key, XXEFinding::line, XXEFinding::column);
    assertThat(result.changes()).isEmpty();
    assertThat(result.unfixedFindings()).isEmpty();
  }
}

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
  void it_doesnt_fix_unknown_parser() {
    String vulnerableCode =
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

    List<Finding> findings = List.of(new Finding("foo", 14, 19));
    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "foo", rule, findings, Finding::getKey, Finding::getLine, Finding::getColumn);
    assertThat(result.changes()).isEmpty();
    assertThat(result.unfixedFindings()).isEmpty();
  }

  @Test
  void it_fixes_xmlreaders_at_parse_call() {
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

    List<Finding> findings = List.of(new Finding("foo", 14, 19));
    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "foo", rule, findings, Finding::getKey, Finding::getLine, Finding::getColumn);
    assertThat(result.unfixedFindings()).isEmpty();
    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(14);

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
                                parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
                                parser.setFeature(VALIDATION, true);
                                parser.setErrorHandler(new MyErrorHandler());
                                parser.setProperty(JAXP_SCHEMA_SOURCE, new File(schemaName));
                                sr = new StringReader(str);
                                parser.setFeature("http://xml.org/sax/features/external-general-entities", false);
                                parser.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
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

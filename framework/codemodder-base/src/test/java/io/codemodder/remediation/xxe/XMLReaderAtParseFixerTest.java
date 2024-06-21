package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.Test;

final class XMLReaderAtParseFixerTest {

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

    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);

    XXEFixAttempt fixAttempt = new XMLReaderAtParseFixer().tryFix(14, 19, cu);

    assertThat(fixAttempt.isFixed()).isTrue();
    assertThat(fixAttempt.isResponsibleFixer()).isTrue();
    assertThat(fixAttempt.reasonNotFixed()).isNullOrEmpty();
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
}

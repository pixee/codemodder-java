package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DocumentBuilderFactoryAtNewDBFixerTest {

  private DocumentBuilderFactoryAtNewDBFixer fixer;

  @BeforeEach
  void setup() {
    this.fixer = new DocumentBuilderFactoryAtNewDBFixer();
  }

  @Test
  void it_fixes_dbf_at_new_db_call() {
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
    XXEFixAttempt attempt = fixer.tryFix(10, null, cu);
    assertThat(attempt.isFixed()).isTrue();
    assertThat(attempt.isResponsibleFixer()).isTrue();

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

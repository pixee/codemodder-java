package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class SAXParserAtNewSPFixerTest {

  private SAXParserAtNewSPFixer fixer;

  @BeforeEach
  void setup() {
    this.fixer = new SAXParserAtNewSPFixer();
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
    XXEFixAttempt attempt = fixer.tryFix(6, null, cu);
    assertThat(attempt.isFixed()).isTrue();
    assertThat(attempt.isResponsibleFixer()).isTrue();

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

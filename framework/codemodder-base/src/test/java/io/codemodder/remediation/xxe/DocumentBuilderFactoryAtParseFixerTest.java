package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class DocumentBuilderFactoryAtParseFixerTest {

  private DocumentBuilderFactoryAtParseFixer fixer;

  private static final String unfixableBecauseDocumentBuilderCameFromOutside =
      """
            public class MyCode {
                  public void foo(DocumentBuilder db) {
                        boolean success;
                        try
                        {
                            db.parse(new InputSource(sr));
                            success = true;
                        } catch (FileNotFoundException e) {
                            success = false;
                            logError(e);
                        }
                  }
                }
            """;

  private static final String unfixableBecauseDocumentBuilderTypeIsntLocal =
      """
            public class MyCode {
                  public void foo() {
                        boolean success;
                        try
                        {
                            getDb().parse(new InputSource(sr));
                            success = true;
                        } catch (FileNotFoundException e) {
                            success = false;
                            logError(e);
                        }
                  }
                  private DocumentBuilder getDb() {
                     return DocumentBuilderFactory.newInstance().newDocumentBuilder();
                  }
                }
            """;

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        Arguments.of(
            unfixableBecauseDocumentBuilderCameFromOutside,
            6,
            19,
            "DocumentBuilder came from outside the method scope"),
        Arguments.of(
            unfixableBecauseDocumentBuilderTypeIsntLocal, 6, null, "No calls at that location"));
  }

  @BeforeEach
  void setup() {
    this.fixer = new DocumentBuilderFactoryAtParseFixer();
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_doesnt_fix(final String code, final int line, final Integer column, final String reason) {
    CompilationUnit cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    XXEFixAttempt attempt = fixer.tryFix(line, column, cu);
    assertThat(attempt.isFixed()).isFalse();
    assertThat(attempt.reasonNotFixed()).isEqualTo(reason);
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

    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);
    XXEFixAttempt attempt = fixer.tryFix(11, 16, cu);
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

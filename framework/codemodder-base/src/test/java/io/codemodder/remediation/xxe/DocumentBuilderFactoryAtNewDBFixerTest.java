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

final class DocumentBuilderFactoryAtNewDBFixerTest {

  private Remediator<Object> fixer;

  @BeforeEach
  void setup() {
    fixer =
        new SearcherStrategyRemediator.Builder<>()
            .withMatchAndFixStrategy(new DocumentBuilderFactoryAtNewDBFixStrategy())
            .build();
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
    var result =
        fixer.remediateAll(
            cu,
            "path",
            new DetectorRule("", "", ""),
            List.of(new Object()),
            o -> "id",
            o -> 10,
            o -> Optional.empty(),
            o -> Optional.empty());
    assertThat(result.changes().isEmpty()).isFalse();

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

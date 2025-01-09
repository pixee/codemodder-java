package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.WithoutScopePositionMatcher;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class DefaultXXERemediatorTest {

  private XXERemediator<XXEFinding> remediator;
  private DetectorRule rule;

  private static Stream<Arguments> fixableSamples() {
    return Stream.of(
        Arguments.of(
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
                          """,
            11,
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
                        """),
        Arguments.of(
            """
               import jakarta.xml.bind.JAXBContext;
               import jakarta.xml.bind.JAXBException;
               import java.io.StringReader;
               import java.time.LocalDateTime;
               import java.time.format.DateTimeFormatter;
               import java.util.ArrayList;
               import java.util.Comparator;
               import java.util.HashMap;
               import java.util.Map;
               import javax.xml.XMLConstants;
               import javax.xml.stream.XMLInputFactory;
               import javax.xml.stream.XMLStreamException;
               import org.owasp.webgoat.container.users.WebGoatUser;
               import org.springframework.context.annotation.Scope;
               import org.springframework.stereotype.Component;

               public class CommentCreator {

                  public Comment comment() {
                    var jc = JAXBContext.newInstance(Comment.class);
                    var xif = XMLInputFactory.newInstance();

                    // TODO fix me disabled for now.
                    if (securityEnabled) {
                      xif.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant
                      xif.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
                    }

                    var xsr = xif.createXMLStreamReader(new StringReader(xml));

                    var unmarshaller = jc.createUnmarshaller();
                    return (Comment) unmarshaller.unmarshal(xsr);
                  }
               }
               """,
            21,
            """
                       import jakarta.xml.bind.JAXBContext;
                       import jakarta.xml.bind.JAXBException;
                       import java.io.StringReader;
                       import java.time.LocalDateTime;
                       import java.time.format.DateTimeFormatter;
                       import java.util.ArrayList;
                       import java.util.Comparator;
                       import java.util.HashMap;
                       import java.util.Map;
                       import javax.xml.XMLConstants;
                       import javax.xml.stream.XMLInputFactory;
                       import javax.xml.stream.XMLStreamException;
                       import org.owasp.webgoat.container.users.WebGoatUser;
                       import org.springframework.context.annotation.Scope;
                       import org.springframework.stereotype.Component;

                       public class CommentCreator {

                          public Comment comment() {
                            var jc = JAXBContext.newInstance(Comment.class);
                            var xif = XMLInputFactory.newInstance();
                            xif.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

                            // TODO fix me disabled for now.
                            if (securityEnabled) {
                              xif.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant
                              xif.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
                            }

                            var xsr = xif.createXMLStreamReader(new StringReader(xml));

                            var unmarshaller = jc.createUnmarshaller();
                            return (Comment) unmarshaller.unmarshal(xsr);
                          }
                       }
                       """));
  }

  @BeforeEach
  void setup() {
    this.remediator = new XXERemediator<>(new WithoutScopePositionMatcher());
    this.rule = new DetectorRule("xxe", "XXE", null);
  }

  @ParameterizedTest
  @MethodSource("fixableSamples")
  void it_fixes_dbf_at_parse_call(String beforeCode, int line, String afterCode) {

    List<XXEFinding> findings = List.of(new XXEFinding("foo", line, null));
    CompilationUnit cu = StaticJavaParser.parse(beforeCode);
    LexicalPreservingPrinter.setup(cu);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "foo",
            rule,
            findings,
            XXEFinding::key,
            XXEFinding::line,
            x -> Optional.empty(),
            x -> Optional.ofNullable(x.column()));
    assertThat(result.unfixedFindings()).isEmpty();
    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(line);

    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualToIgnoringCase(afterCode);
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
            cu,
            "foo",
            rule,
            findings,
            XXEFinding::key,
            XXEFinding::line,
            x -> Optional.empty(),
            x -> Optional.ofNullable(x.column()));
    assertThat(result.changes()).isEmpty();
  }
}

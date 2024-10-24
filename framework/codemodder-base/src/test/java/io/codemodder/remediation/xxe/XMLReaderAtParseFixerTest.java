package io.codemodder.remediation.xxe;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.SearcherStrategyRemediator;
import io.codemodder.remediation.WithoutScopePositionMatcher;
import java.util.List;
import java.util.Optional;
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

    var searcherRemediator =
        new SearcherStrategyRemediator.Builder<>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<Object>()
                    .withMatcher(XMLReaderAtParseFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new XMLReaderAtParseFixStrategy())
            .build();
    var result =
        searcherRemediator.remediateAll(
            cu,
            "path",
            new DetectorRule("", "", ""),
            List.of(new Object()),
            o -> "id",
            o -> 14,
            o -> Optional.empty(),
            o -> Optional.of(19));

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

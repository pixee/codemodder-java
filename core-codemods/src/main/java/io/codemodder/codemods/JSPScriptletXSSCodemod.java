package io.codemodder.codemods;

import io.codemodder.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * This type corrects simple and obvious XSS vulnerabilities in JSPs. It looks for patterns like:
 *
 * <p>&lt;%= request.getParameter("anything") %&gt;
 *
 * <p>Literally almost any modification to the above snippets could provide protection -- we only
 * want to find 100% certain cases when its uncontrolled user input which can escape any context and
 * achieve exploitation.
 */
@Codemod(
    id = "pixee:java/encode-jsp-scriptlet",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class JSPScriptletXSSCodemod extends RegexFileChanger {

  private IncludesExcludesPattern includesExcludesPattern;

  public JSPScriptletXSSCodemod() {
    super(scriptlet, true, List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER));
    this.includesExcludesPattern =
        new IncludesExcludesPattern.Default(Set.of("**.[jJ][sS][pP]"), Set.of());
  }

  @Override
  public String getReplacementFor(final String matchingSnippet) {
    var codeWithinScriptlet =
        matchingSnippet.substring(matchingSnippet.indexOf('=') + 1, matchingSnippet.length() - 2);
    return "<%=org.owasp.encoder.Encode.forHtml(" + codeWithinScriptlet + ")%>";
  }

  @Override
  public boolean supports(final Path file) {
    return file.getFileName().toString().toLowerCase().endsWith(".jsp");
  }

  private static final Pattern scriptlet =
      Pattern.compile(
          "<%(\\s*)=(\\s*)request(\\s*).(\\s*)get((Header|Parameter)(\\s*)\\((\\s*)\".*\"(\\s*)\\)|QueryString\\((\\s*)\\))(\\s*)%>",
          Pattern.MULTILINE);

  @Override
  public IncludesExcludesPattern getIncludesExcludesPattern() {
    return includesExcludesPattern;
  }
}

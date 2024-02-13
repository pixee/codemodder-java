package io.codemodder.codemods;

import io.codemodder.*;
import java.util.List;
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

  public JSPScriptletXSSCodemod() {
    super(
        path -> path.getFileName().toString().toLowerCase().endsWith(".jsp"),
        scriptlet,
        true,
        List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER));
  }

  @Override
  public String getReplacementFor(final String matchingSnippet) {
    var codeWithinScriptlet =
        matchingSnippet.substring(matchingSnippet.indexOf('=') + 1, matchingSnippet.length() - 2);
    return "<%=org.owasp.encoder.Encode.forHtml(" + codeWithinScriptlet + ")%>";
  }

  private static final Pattern scriptlet =
      Pattern.compile(
          "<%(\\s*)=(\\s*)request(\\s*).(\\s*)get((Header|Parameter)(\\s*)\\((\\s*)\".*\"(\\s*)\\)|QueryString\\((\\s*)\\))(\\s*)%>",
          Pattern.MULTILINE);
}

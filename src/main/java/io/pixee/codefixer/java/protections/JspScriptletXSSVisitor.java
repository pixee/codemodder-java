package io.pixee.codefixer.java.protections;

import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;

import com.google.common.annotations.VisibleForTesting;
import io.pixee.codefixer.java.DependencyGAV;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * This type corrects simple and obvious XSS vulnerabilities in JSPs. It looks for patterns like:
 *
 * <p><%= request.getParameter("anything") %>
 *
 * <p>Literally almost any modification to the above snippets could provide protection -- we only
 * want to find 100% certain cases when its uncontrolled user input which can escape any context and
 * achieve exploitation.
 */
public final class JspScriptletXSSVisitor extends RegexTextVisitor {

  public JspScriptletXSSVisitor() {
    super(
        file -> endsWithIgnoreCase(file.getName(), ".jsp"),
        scriptlet,
        xssJspScriptletRuleId,
        DependencyGAV.OWASP_XSS_JAVA_ENCODER);
  }

  @Override
  public String ruleId() {
    return xssJspScriptletRuleId;
  }

  @Override
  public @NotNull String getReplacementFor(final String matchingSnippet) {
    var codeWithinScriptlet =
        matchingSnippet.substring(matchingSnippet.indexOf('=') + 1, matchingSnippet.length() - 2);
    return "<%=org.owasp.encoder.Encode.forHtml(" + codeWithinScriptlet + ")%>";
  }

  private static final Pattern scriptlet =
      Pattern.compile(
          "<%(\\s*)=(\\s*)request(\\s*).(\\s*)get((Header|Parameter)(\\s*)\\((\\s*)\".*\"(\\s*)\\)|QueryString\\((\\s*)\\))(\\s*)%>",
          Pattern.MULTILINE);
  @VisibleForTesting static final String xssJspScriptletRuleId = "pixee:java/encode-jsp-scriptlet";
}

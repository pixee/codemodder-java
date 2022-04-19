package io.pixee.codefixer.java.plugins.contrast;

import io.pixee.codefixer.java.JspLineWeave;
import org.codehaus.plexus.util.StringUtils;

/**
 * For JSP expressions like:
 *
 * <pre>&lt;%= variable %&gt;</pre>
 */
final class JspExpressionOutputMethod implements JspOutputMethod {

  @Override
  public int countPossibleWrites(final String line) {
    return StringUtils.countMatches(line, "<%=");
  }

  @Override
  public JspLineWeave weaveLine(final String line, final String ruleId) {
    int startIndex = line.indexOf("<%=");
    int endIndex = line.indexOf("%>", startIndex);
    if (endIndex == -1) {
      throw new IllegalArgumentException("no end to JSP expression on line");
    }
    String thingToEncode = line.substring(startIndex + 3, endIndex);
    StringBuilder sb = new StringBuilder();
    sb.append(line, 0, startIndex);
    sb.append("<%=io.pixee.security.XSS.htmlEncode(String.valueOf(");
    sb.append(thingToEncode.trim());
    sb.append("))%>");
    sb.append(line, endIndex + 2, line.length());
    return new JspLineWeave(sb.toString(), null, ruleId);
  }
}

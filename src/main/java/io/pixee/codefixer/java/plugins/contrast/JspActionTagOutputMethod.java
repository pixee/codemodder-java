package io.pixee.codefixer.java.plugins.contrast;

import io.pixee.codefixer.java.JspLineWeave;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For JSP action tags like:
 *
 * <pre>&lt;c:out name=&quot;variable&quot;/%gt;</pre>
 */
final class JspActionTagOutputMethod implements JspOutputMethod {

  @Override
  public int countPossibleWrites(final String line) {
    Matcher matcher = jspActionTag.matcher(line);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  @Override
  public JspLineWeave weaveLine(final String line, final String ruleId) {
    throw new UnsupportedOperationException(JspActionTagOutputMethod.class.getSimpleName());
  }

  private static final Pattern jspActionTag = Pattern.compile("<[a-zA-Z]+:[a-zA-Z]+");
}

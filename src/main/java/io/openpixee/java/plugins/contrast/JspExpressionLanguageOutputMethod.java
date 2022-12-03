package io.openpixee.java.plugins.contrast;

import io.openpixee.java.JspLineWeave;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.codehaus.plexus.util.StringUtils;

/**
 * For expression language like:
 *
 * <pre>${variable}</pre>
 */
final class JspExpressionLanguageOutputMethod implements JspOutputMethod {

  private String fnPrefix;

  JspExpressionLanguageOutputMethod(final List<String> allLines) {
    Objects.requireNonNull(allLines);
    this.fnPrefix = null;
    for (String line : allLines) {
      Optional<JspTaglibDefinition> tagLibDef = JspTaglibDefinition.parseFrom(line);
      if (tagLibDef.isPresent()) {
        JspTaglibDefinition taglib = tagLibDef.get();
        if ("http://java.sun.com/jsp/jstl/functions".equals(taglib.getUri())) {
          this.fnPrefix = taglib.getPrefix();
        }
      }
    }
  }

  @Override
  public int countPossibleWrites(final String line) {
    return StringUtils.countMatches(line, "${");
  }

  @Override
  public JspLineWeave weaveLine(final String line, final String ruleId) {
    final String prefixToUse;
    final String taglibToAdd;
    if (fnPrefix == null) {
      prefixToUse = "fn";
      taglibToAdd =
          "<%@ taglib uri = \"http://java.sun.com/jsp/jstl/functions\" prefix = \"fn\" %>";
    } else {
      prefixToUse = fnPrefix;
      taglibToAdd = null;
    }
    int startIndex = line.indexOf("${");
    int endIndex = line.indexOf("}", startIndex + 2);
    if (endIndex == -1 || endIndex - startIndex <= 2) {
      throw new UnsupportedOperationException("empty expression");
    }
    String rebuilt =
        line.substring(0, startIndex)
            + "${"
            + prefixToUse
            + ":escapeXml("
            + line.substring(startIndex + 2, endIndex)
            + ")}"
            + line.substring(endIndex + 1);
    return new JspLineWeave(rebuilt, taglibToAdd, ruleId);
  }
}

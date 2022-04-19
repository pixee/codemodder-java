package io.pixee.codefixer.java.plugins.contrast;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Contains a simple definition of a JSP taglib. */
final class JspTaglibDefinition {
  private final String uri;
  private final String prefix;

  private JspTaglibDefinition(final String uri, final String prefix) {
    this.uri = Objects.requireNonNull(uri);
    this.prefix = Objects.requireNonNull(prefix);
  }

  String getUri() {
    return uri;
  }

  String getPrefix() {
    return prefix;
  }

  static Optional<JspTaglibDefinition> parseFrom(final String jspLine) {
    Objects.requireNonNull(jspLine);
    Matcher matcher = jspTagLibDef.matcher(jspLine);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(new JspTaglibDefinition(matcher.group(6), matcher.group(10)));
  }

  private static final Pattern jspTagLibDef =
      Pattern.compile(
          "(\\s)*<%@(\\s)*taglib(\\s)*uri(\\s)*=(\\s)*\"(.*)\"(\\s)*prefix(\\s)*=(\\s)*\"(.*)\"(\\s)*%>.*");
}

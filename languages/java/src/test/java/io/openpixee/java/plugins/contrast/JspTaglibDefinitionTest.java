package io.openpixee.java.plugins.contrast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JspTaglibDefinitionTest {

  @ParameterizedTest
  @MethodSource("tagDefinitions")
  void it_parses_tag_lib_definitions(
      final String taglib, final boolean parses, final String uri, final String prefix) {
    Optional<JspTaglibDefinition> tagDef = JspTaglibDefinition.parseFrom(taglib);
    assertThat(tagDef.isPresent(), is(parses));
    if (parses) {
      JspTaglibDefinition tag = tagDef.get();
      assertThat(tag.getUri(), equalTo(uri));
      assertThat(tag.getPrefix(), equalTo(prefix));
    }
  }

  static Stream<Arguments> tagDefinitions() {
    return Stream.of(
        Arguments.of(
            "<%@ taglib uri = \"http://www.example.com/custlib\" prefix = \"mytag\" %>",
            true, "http://www.example.com/custlib", "mytag"),
        Arguments.of("\t<%@taglib uri =\"https://uri\" prefix=\"fn\"%>", true, "https://uri", "fn"),
        Arguments.of("\t<%@somethingElse uri =\"https://uri\" prefix=\"fn\"%>", false, null, null),
        Arguments.of("\t<%@taglib notRight =\"https://uri\" prefix=\"fn\"%>", false, null, null),
        Arguments.of("\t<%@taglib uri =\"https://uri\" notright=\"fn\"%>", false, null, null),
        Arguments.of(
            "\t<%@taglib uri =\"https://uri\" prefix='fn'%>",
            false, null, null) // single quotes unsupported
        );
  }
}

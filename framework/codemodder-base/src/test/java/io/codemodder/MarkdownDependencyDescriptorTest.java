package io.codemodder;

import static io.codemodder.DependencyGAV.createDefault;
import static org.hamcrest.Matchers.equalTo;

import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class MarkdownDependencyDescriptorTest {

  private MarkdownDependencyDescriptor descriptor;

  private static Stream<Arguments> testCases() {
    String group = "org.owasp";
    String artifact = "owasp-java-html-sanitizer";
    String version = "2019.2";
    String justification = "We need this to sanitize HTML";
    String repoUrl = "https://github.com/owasp/sanitizer";
    String license = "Apache 2.0";

    return Stream.of(
        // has everything
        Arguments.of(
            createDefault(group, artifact, version, null, justification, license, repoUrl, true),
            """
                        We need this to sanitize HTML

                        License: Apache 2.0 ✅ | [Open source](https://github.com/owasp/sanitizer) ✅ | No transitive dependencies ✅ | [More facts](https://mvnrepository.com/artifact/org.owasp/owasp-java-html-sanitizer/2019.2)
                        """),
        // no description
        Arguments.of(
            createDefault(group, artifact, version, null, null, license, repoUrl, true),
            """
                        This dependency change is required to use the new code.

                        License: Apache 2.0 ✅ | [Open source](https://github.com/owasp/sanitizer) ✅ | No transitive dependencies ✅ | [More facts](https://mvnrepository.com/artifact/org.owasp/owasp-java-html-sanitizer/2019.2)
                        """),
        // unknown license
        Arguments.of(
            createDefault(
                group, artifact, version, justification, null, "FakeLicense 1.3", repoUrl, true),
            """
                        We need this to sanitize HTML

                        License: FakeLicense 1.3 ❓ | [Open source](https://github.com/owasp/sanitizer) ✅ | No transitive dependencies ✅ | [More facts](https://mvnrepository.com/artifact/org.owasp/owasp-java-html-sanitizer/2019.2)
                        """),
        // no license
        Arguments.of(
            createDefault(group, artifact, version, null, justification, null, repoUrl, true),
            """
                        We need this to sanitize HTML

                        [Open source](https://github.com/owasp/sanitizer) ✅ | No transitive dependencies ✅ | [More facts](https://mvnrepository.com/artifact/org.owasp/owasp-java-html-sanitizer/2019.2)
                        """),
        // no repo url
        Arguments.of(
            createDefault(group, artifact, version, null, justification, null, null, true),
            """
                        We need this to sanitize HTML

                        No transitive dependencies ✅ | [More facts](https://mvnrepository.com/artifact/org.owasp/owasp-java-html-sanitizer/2019.2)
                        """),
        // add repo url back, and now there's transitive deps
        Arguments.of(
            createDefault(group, artifact, version, null, justification, null, repoUrl, false),
            """
                        We need this to sanitize HTML

                        [Open source](https://github.com/owasp/sanitizer) ✅ | [More facts](https://mvnrepository.com/artifact/org.owasp/owasp-java-html-sanitizer/2019.2)
                        """));
  }

  @BeforeEach
  void setup() {
    this.descriptor = new MarkdownDependencyDescriptor();
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void it_marks_up_fully(final DependencyGAV dep, final String expectedOutput) {
    MatcherAssert.assertThat(descriptor.create(dep), equalTo(expectedOutput));
  }
}

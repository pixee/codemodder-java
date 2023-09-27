package io.codemodder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.io.IOUtils;

/** A type responsible for reporting codemod changes. */
public interface CodemodReporterStrategy {

  String getSummary();

  String getDescription();

  String getChange(Path path, CodemodChange change);

  List<String> getReferences();

  /**
   * A {@link CodemodReporterStrategy} that reports based on text from a predictable location on
   * classpath. This is an alternative to storing data inline to the Java source code of your {@link
   * CodeChanger}. It's easier to maintain this "data" outside of code, so we prefer a simple
   * mechanism for doing that. Both the files read are expected to be in
   *
   * <pre>/com/acme/MyCodemod/</pre>
   *
   * (assuming that's the name of your codemod type.)
   *
   * <p>The first expected file in that directory is {@code report.json}. It contains most of the
   * fields we want to report:
   *
   * <pre>{@code
   * {
   *     "summary": "A headline describing the changes provided by this codemod",
   *     "control": "A URL linking to the source of the security control added",
   *     "change": "A description of the change suitable for a particular instance of a change",
   *     "references": [
   *          "A URL for further reading",
   *          "Another URL for further reading"
   *     ]
   * }
   * }</pre>
   *
   * <p>The second file is ${@code description.md}, and it provides last field needed by a {@link
   * CodemodReporterStrategy } is the description of the codemod itself. This is expected to be a
   * bigger piece of text, and thus it is stored in a separate file where it can be easily edited
   * with an IDE supporting markdown.
   *
   * <p>Thus, in a typical Java project, using this {@link CodemodReporterStrategy } would mean your
   * artifact would retain at least these 3 files:
   *
   * <ul>
   *   <li>src/main/java/com/acme/MyCodemod.java
   *   <li>src/main/resources/com/acme/MyCodemod/report.json
   *   <li>src/main/resources/com/acme/MyCodemod/description.mdli>
   * </ul>
   *
   * @param codemodType the {@link CodeChanger} type to load the reporting text for
   * @return a {@link CodemodReporterStrategy } that reports based on text from the classpath
   */
  static CodemodReporterStrategy fromClasspath(final Class<? extends CodeChanger> codemodType) {

    Objects.requireNonNull(codemodType);

    // create the expected paths to the files
    String descriptionResource = "/" + codemodType.getName().replace('.', '/') + "/description.md";
    String reportJson = "/" + codemodType.getName().replace('.', '/') + "/report.json";

    // load the reporting text
    ObjectMapper mapper = new ObjectMapper();
    final JsonNode parent;
    final String description;
    try {
      var jsonResource = codemodType.getResourceAsStream(reportJson);
      var mdResource = codemodType.getResourceAsStream(descriptionResource);
      if (jsonResource == null || mdResource == null) {
        throw new IllegalArgumentException(
            "Could not find report.json or description.md for: " + codemodType);
      }
      parent = mapper.readTree(jsonResource);
      description = IOUtils.toString(mdResource);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    // pull the right data out
    String summary = parent.get("summary").asText();
    String change = parent.get("change").asText();
    ArrayNode referencesNode = (ArrayNode) parent.get("references");
    List<String> references =
        StreamSupport.stream(referencesNode.spliterator(), false)
            .map(JsonNode::asText)
            .collect(Collectors.toList());

    return new CodemodReporterStrategy() {
      @Override
      public String getSummary() {
        return summary;
      }

      @Override
      public String getDescription() {
        return description;
      }

      @Override
      public String getChange(final Path path, final CodemodChange codeChange) {
        return change;
      }

      @Override
      public List<String> getReferences() {
        return references;
      }
    };
  }

  static CodemodReporterStrategy empty() {
    return new CodemodReporterStrategy() {
      @Override
      public String getSummary() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getDescription() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getChange(Path path, CodemodChange change) {
        throw new UnsupportedOperationException();
      }

      @Override
      public List<String> getReferences() {
        throw new UnsupportedOperationException();
      }
    };
  }
}

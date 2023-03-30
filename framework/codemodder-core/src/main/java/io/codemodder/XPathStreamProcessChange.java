package io.codemodder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/** Represents the results when an XML file is transformed using {@link XPathStreamProcessor}. */
public interface XPathStreamProcessChange {

  /** The lines that were affected by the change. */
  Set<Integer> linesAffected();

  /** A temporary file containing the transformed XML. */
  Path transformedXml();

  class Default implements XPathStreamProcessChange {
    private final Set<Integer> linesAffected;
    private final Path transformedXml;

    public Default(final Set<Integer> linesAffected, final Path transformedXml) {
      this.linesAffected = Objects.requireNonNull(linesAffected);
      this.transformedXml = Objects.requireNonNull(transformedXml);
    }

    @Override
    public Set<Integer> linesAffected() {
      return Collections.unmodifiableSet(linesAffected);
    }

    @Override
    public Path transformedXml() {
      return transformedXml;
    }
  }
}

package io.openpixee.java;

import com.github.javaparser.ast.Node;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** This is the context which is passed into each weave. */
public interface FileWeavingContext {

  /** This is called during our scanning process when we have successfully weaved in a change. */
  void addWeave(Weave weave);

  /** Return a list of successful weaves. */
  List<Weave> weaves();

  boolean madeWeaves();

  boolean isLineIncluded(Node n);

  class Default implements FileWeavingContext {

    private final List<Weave> weaves;
    private final LineIncludesExcludes includesExcludes;
    private final File file;

    Default(final File file, final LineIncludesExcludes includesExcludes) {
      this.file = file;
      this.includesExcludes = includesExcludes;
      this.weaves = new ArrayList<>();
    }

    @Override
    public List<Weave> weaves() {
      return weaves;
    }

    @Override
    public void addWeave(Weave weave) {
      weaves.add(weave);
    }

    @Override
    public boolean madeWeaves() {
      return !weaves.isEmpty();
    }

    @Override
    public boolean isLineIncluded(final Node n) {
      if (n.getRange().isEmpty()) {
        return true;
      }
      return includesExcludes.matches(n.getRange().get().begin.line);
    }
  }

  static FileWeavingContext createDefault(
      final File file, final IncludesExcludes includesExcludes) {
    return new Default(file, includesExcludes.getIncludesExcludesForFile(file));
  }
}

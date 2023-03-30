package io.codemodder;

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

  /** Intended to be used in non-JavaParser situations. */
  boolean isLineIncluded(int line);

  class Default implements FileWeavingContext {

    private final List<Weave> weaves;
    private final LineIncludesExcludes includesExcludes;

    Default(final LineIncludesExcludes includesExcludes) {
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
    public boolean isLineIncluded(final int line) {
      return includesExcludes.matches(line);
    }
  }

  static FileWeavingContext createDefault(
      final File file, final IncludesExcludes includesExcludes) {
    return new Default(includesExcludes.getIncludesExcludesForFile(file));
  }

  static FileWeavingContext createDefault(final LineIncludesExcludes includesExcludesForFile) {
    return new Default(includesExcludesForFile);
  }
}

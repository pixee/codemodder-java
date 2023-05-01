package io.codemodder;

/** A codemod and its ID. */
public final class CodemodIdPair {

  private final String id;
  private final Changer changer;

  public CodemodIdPair(final String id, final Changer changer) {
    this.id = id;
    this.changer = changer;
  }

  public String getId() {
    return id;
  }

  public Changer getChanger() {
    return changer;
  }
}

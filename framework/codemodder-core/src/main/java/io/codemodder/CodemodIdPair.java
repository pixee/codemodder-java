package io.codemodder;

/** A codemod and its ID. */
public final class CodemodIdPair {

  private final String id;
  private final CodeChanger codeChanger;

  public CodemodIdPair(final String id, final CodeChanger codeChanger) {
    this.id = id;
    this.codeChanger = codeChanger;
  }

  public String getId() {
    return id;
  }

  public CodeChanger getChanger() {
    return codeChanger;
  }
}

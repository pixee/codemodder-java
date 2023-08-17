package io.codemodder;

/** A type that describes a new dependency being added to a project. */
public interface DependencyDescriptor {

  /**
   * Creates a description for a dependency that a codemod wants to add to the project. It should
   * help the developer who is considering the change make an informed decision about accepting the
   * change.
   */
  String create(DependencyGAV dependency);

  /** Create a descriptor that uses markdown. */
  static DependencyDescriptor createMarkdownDescriptor() {
    return new MarkdownDependencyDescriptor();
  }
}

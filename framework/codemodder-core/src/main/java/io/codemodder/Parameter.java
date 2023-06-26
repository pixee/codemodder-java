package io.codemodder;

import java.nio.file.Path;

/** A parameter for a codemod, capable of retrieving the right value for a given path and line. */
public interface Parameter {

  /** Get the value of a given codemod parameter for the given path and line. */
  String getValue(Path path, int currentLine);

  /** Get the description of the codemod parameter. */
  String getDescription();

  /** Get the default value of the codemod parameter. */
  String getDefaultValue();
}

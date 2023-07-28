package io.codemodder;

import java.nio.file.Path;

/** A parameter for a codemod, capable of retrieving the right value for a given path and line. */
public interface Parameter {

  /** Get the question to ask the user for the value. */
  String getQuestion();

  /** Get the name of the variable. */
  String getName();

  /** Get the type of parameter, should be "string", "number", or "boolean". */
  String getType();

  /** Get the value of a given codemod parameter for the given path and line. */
  String getValue(Path path, int currentLine);

  /** Get a description of the codemod parameter. */
  String getLabel();

  /** Get the default value of the codemod parameter. */
  String getDefaultValue();
}

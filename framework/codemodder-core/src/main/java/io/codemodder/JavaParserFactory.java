package io.codemodder;

import com.github.javaparser.JavaParser;
import java.io.IOException;
import java.nio.file.Path;

/** Responsible for generating {@link JavaParser} instances. */
interface JavaParserFactory {

  /**
   * Create a JavaParser instance for the given project path that understands the project structure.
   *
   * @param projectPath the path to the project
   * @return a JavaParser instance that is setup to understand the project's types and anything else
   *     it needs
   */
  JavaParser create(Path projectPath) throws IOException;
}

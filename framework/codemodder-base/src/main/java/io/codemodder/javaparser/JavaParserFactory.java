package io.codemodder.javaparser;

import com.github.javaparser.JavaParser;
import io.codemodder.SourceDirectory;
import java.io.IOException;
import java.util.List;

/** Responsible for generating {@link JavaParser} instances. */
public interface JavaParserFactory {

  /**
   * Create a JavaParser instance for the given project source directories
   *
   * @param sourceDirectories the path to the project
   * @return a JavaParser instance based on the given source directories
   */
  JavaParser create(List<SourceDirectory> sourceDirectories) throws IOException;

  static JavaParserFactory newFactory() {
    return new DefaultJavaParserFactory();
  }
}

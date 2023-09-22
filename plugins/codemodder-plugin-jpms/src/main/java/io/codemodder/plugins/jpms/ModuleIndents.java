package io.codemodder.plugins.jpms;

import com.github.javaparser.Range;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleDirective;
import io.codemodder.DependencyGAV;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A type responsible for helping to fixing up indent of a file based on what exists already. I can
 * see this type eventually growing to solve more types of problems, and when it does, it will be a
 * good candidate to move to a more core module. Fixing up whitespace is a problem we've been warned
 * about by JavaParser devs we may continue to have to deal with.
 */
final class ModuleIndents {

  private ModuleIndents() {} // static only

  /**
   * This hacky code addresses an issue with JavaParser's concrete syntax support for inserting new
   * module directives.
   *
   * @param moduleInfoJava the path to the module-info.java file
   * @param rawAfter the string containing the module-info.java file contents after the change
   * @param moduleDeclaration the module declaration that was modified
   * @param successfullyInjectedDependencies the dependencies that were injected
   * @return a version that may have the spacing adjusted to indent the newly added lines
   */
  static String tryFixUpSpacing(
      final Path moduleInfoJava,
      final String rawAfter,
      final ModuleDeclaration moduleDeclaration,
      final List<DependencyGAV> successfullyInjectedDependencies) {
    // find the first module directive _we didn't add_ so that we can figure out the indentation
    NodeList<ModuleDirective> directives = moduleDeclaration.getDirectives();
    List<String> injectedModuleNames =
        successfullyInjectedDependencies.stream()
            .map(DependencyGAV::moduleName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    Optional<ModuleDirective> firstOriginalModuleDirective =
        directives.stream()
            .filter(
                d ->
                    !d.isModuleRequiresDirective()
                        || !injectedModuleNames.contains(
                            d.asModuleRequiresDirective().getNameAsString()))
            .findFirst();

    String indentation = " ".repeat(2); // default and fallback to 2 spaces

    if (firstOriginalModuleDirective.isPresent()) {
      try {
        ModuleDirective originalModuleDirective = firstOriginalModuleDirective.get();
        Optional<Range> range = originalModuleDirective.getRange();
        if (range.isPresent()) {
          indentation = getIndentationAtLine(moduleInfoJava, range.get().begin.line);
        }
      } catch (IOException e) {
        log.debug("Problem parsing original line indentation for module insertion", e);
      }
    }

    String fixedUpAfter = rawAfter;
    for (DependencyGAV d : successfullyInjectedDependencies) {
      String rawStatement = "requires " + d.moduleName().get() + ";";
      String token = "\n" + rawStatement;
      fixedUpAfter = fixedUpAfter.replace(token, "\n" + indentation + rawStatement);
    }

    return fixedUpAfter;
  }

  /**
   * Get the indentation (leading whitespace) at the given line of the given file.
   *
   * @param file the code file to read from
   * @param line the 1-based line number to read
   */
  private static String getIndentationAtLine(final Path file, final int line) throws IOException {
    List<String> lines = Files.readAllLines(file);
    String lineText = lines.get(line - 1);
    StringBuilder indent = new StringBuilder();
    for (int i = 0; i < lineText.length(); i++) {
      char c = lineText.charAt(i);
      if (Character.isWhitespace(c)) {
        indent.append(c);
      } else {
        break;
      }
    }
    return indent.toString();
  }

  private static final Logger log = LoggerFactory.getLogger(ModuleIndents.class);
}

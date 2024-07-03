package io.codemodder.remediation.xss;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/** A function that can fix XSS that has a particular code shape. */
interface XSSCodeShapeFixer {

  /**
   * Find the shape of code at this location, and fix it if it's the expected shape.
   *
   * @param cu the compilation unit to fix
   * @param path the path of the file being fixed
   * @param detectorRule the detector rule that found the issue
   * @param fixGroup the group of issues to fix
   * @param getKey a function to get the key of an issue
   * @param getLine a function to get the line of an issue
   * @param getColumn a function to get the column of an issue
   * @return the result of the fix
   */
  <T> XSSCodeShapeFixResult fixCodeShape(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> fixGroup,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);
}

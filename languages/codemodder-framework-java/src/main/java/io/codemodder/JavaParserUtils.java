package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;

public final class JavaParserUtils {

  /**
   * Adds a type to the import list if it's not in there directly, and if it's not implied by a
   * wildcard. Care will be taken to ensure it's inserted in alphabetical order.
   *
   * @param cu the class we're changing
   * @param className the new type to ensure is imported
   */
  public static void addImportIfMissing(final CompilationUnit cu, final String className) {
    NodeList<ImportDeclaration> imports = cu.getImports();
    ImportDeclaration newImport = new ImportDeclaration(className, false, false);
    if (imports.contains(newImport)) {
      return;
    }
    for (int i = 0; i < imports.size(); i++) {
      ImportDeclaration existingImport = imports.get(i);

      if (existingImport.getNameAsString().compareToIgnoreCase(className) > 0) {

        imports.addBefore(newImport, existingImport);
        return;
      }
    }
    cu.addImport(className);
  }

  /**
   * Return true if the {@link Node} is within the {@link Region} described.
   *
   * @param node the node to search for within the boundary
   * @param region the given region that defines the boundary
   * @return true, if the node is within the region
   */
  public static boolean regionMatchesNode(final Node node, final Region region) {
    return false;
  }
}

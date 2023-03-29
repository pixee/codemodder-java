package io.openpixee.java;

import static java.util.Collections.unmodifiableMap;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import java.util.HashMap;
import java.util.Map;

/**
 * This locator makes an assumption that any bare class referenced in the imports is the same. I
 * actually don't know what would take precedence between an import and a package, or when wildcards
 * are used, so this may produce incorrect behaviors.
 */
final class ImportBasedTypeLocator implements TypeLocator {

  private final Map<String, String> typeReferences;

  ImportBasedTypeLocator(final NodeList<ImportDeclaration> imports) {
    final Map<String, String> unqualifiedTypeReferences = new HashMap<>();
    for (ImportDeclaration codeImport : imports) {
      String rawClassName = codeImport.getNameAsString();
      int classWithoutPackageIdx = rawClassName.lastIndexOf('.') + 1;
      String simpleClassName = rawClassName.substring(classWithoutPackageIdx);
      unqualifiedTypeReferences.put(simpleClassName, rawClassName);
      unqualifiedTypeReferences.put(rawClassName, rawClassName);
    }
    this.typeReferences = unmodifiableMap(unqualifiedTypeReferences);
  }

  @Override
  public String locateType(final Expression expr) {
    return typeReferences.get(expr.toString());
  }
}

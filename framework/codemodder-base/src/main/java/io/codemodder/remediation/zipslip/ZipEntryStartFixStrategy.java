package io.codemodder.remediation.zipslip;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

/** Fixes ZipSlip vulnerabilities where a ZipEntry starts the data flow. */
final class ZipEntryStartFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    MethodCallExpr call = (MethodCallExpr) node;
    Optional<ClassOrInterfaceDeclaration> classDeclRef =
        call.findAncestor(ClassOrInterfaceDeclaration.class);
    if (classDeclRef.isEmpty()) {
      return SuccessOrReason.reason("No encompassing class found");
    }

    addSanitizeName(classDeclRef.get());
    wrap(call).withScopelessMethod("sanitizeZipFilename");

    return SuccessOrReason.success();
  }

  private static void addSanitizeName(final ClassOrInterfaceDeclaration classDecl) {
    final String method =
        """
                String sanitizeZipFilename(String entryName) {
                    if (entryName == null || entryName.trim().isEmpty()) {
                        return entryName;
                    }
                    while (entryName.contains("../") || entryName.contains("..\\\\")) {
                        entryName = entryName.replace("../", "").replace("..\\\\", "");
                    }
                    return entryName;
                }
                """;
    boolean sanitizeMethodPresent =
        classDecl.findAll(MethodDeclaration.class).stream()
            .anyMatch(
                md ->
                    md.getNameAsString().equals("sanitizeZipFilename")
                        && md.getParameters().size() == 1
                        && md.getParameters().get(0).getTypeAsString().equals("String"));
    if (!sanitizeMethodPresent) {
      classDecl.addMember(StaticJavaParser.parseMethodDeclaration(method));
    }
  }

  /** Return true if it appears to be a ZipEntry#getName() call. */
  static boolean match(final Node node) {
    return node instanceof MethodCallExpr call
        && call.getScope().isPresent()
        && "getName".equals(call.getNameAsString());
  }
}

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

    Optional<MethodDeclaration> methodAncestor = call.findAncestor(MethodDeclaration.class);
    if (methodAncestor.isEmpty()) {
      return SuccessOrReason.reason("No encompassing method found");
    }

    boolean addStatic = methodAncestor.get().isStatic() || classDeclRef.get().isInterface();

    addSanitizeName(classDeclRef.get(), addStatic);
    wrap(call).withScopelessMethod("sanitizeZipFilename");

    return SuccessOrReason.success();
  }

  private static void addSanitizeName(
      final ClassOrInterfaceDeclaration classDecl, final boolean addStatic) {
    String method =
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

    if (addStatic) {
      method = "static " + method;
    }

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
    return Optional.of(node)
        .map(n -> n instanceof MethodCallExpr mce ? mce : null)
        .filter(mce -> mce.hasScope())
        .filter(mce -> "getName".equals(mce.getNameAsString()))
        // Not already sanitized
        .filter(
            mce ->
                mce.getParentNode()
                    .map(p -> p instanceof MethodCallExpr m ? m : null)
                    .filter(m -> "sanitizeZipFilename".equals(m.getNameAsString()))
                    .isEmpty())
        .isPresent();
  }
}

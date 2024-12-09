package io.codemodder.remediation.regexdos;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalDeclaration;
import io.codemodder.ast.LocalVariableDeclaration;
import io.codemodder.remediation.MatchAndFixStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** Adds a timeout function and wraps regex match call with it * */
final class RegexDoSFixStrategy extends MatchAndFixStrategy {

  private final String DEFAULT_TIMEOUT = "5000";

  private static final List<String> matchingMethods =
      List.of("matches", "find", "replaceAll", "replaceFirst");

  /**
   * Test if the node is a Pattern.matcher*() call
   *
   * @param node
   * @return
   */
  @Override
  public boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof Expression e ? e : null)
        .flatMap(ASTs::isArgumentOfMethodCall)
        .filter(mce -> "matcher".equals(mce.getNameAsString()))
        .flatMap(mce -> mce.getScope())
        // Check if the type is Pattern
        .filter(
            scope ->
                ASTs.calculateResolvedType(scope)
                    .filter(t -> "java.util.regex.Pattern".equals(t.describe()))
                    .isPresent())
        .isPresent();
  }

  private static void addTimeoutMethodIfMissing(
      final CompilationUnit cu, final ClassOrInterfaceDeclaration classDecl) {
    final String method =
        """
                public <E> E executeWithTimeout(final Callable<E> action, final int timeout){
                  Future<E> maybeResult = Executors.newSingleThreadExecutor().submit(action);
                  try{
                      return maybeResult.get(timeout, TimeUnit.MILLISECONDS);
                  }catch(Exception e){
                      throw new RuntimeException("Failed to execute within time limit.");
                  }
                }
            """;
    boolean filterMethodPresent =
        classDecl.findAll(MethodDeclaration.class).stream()
            .anyMatch(
                md ->
                    md.getNameAsString().equals("executeWithTimeout")
                        && md.getParameters().size() == 2);
    if (!filterMethodPresent) {
      classDecl.addMember(StaticJavaParser.parseMethodDeclaration(method));
    }
    // Add needed import
    ASTTransforms.addImportIfMissing(cu, Callable.class.getName());
    ASTTransforms.addImportIfMissing(cu, Executors.class.getName());
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    // indirect case, assigned to a variable
    // We know this to be a Pattern.matcher() call from the match method
    MethodCallExpr call = (MethodCallExpr) ASTs.isArgumentOfMethodCall((Expression) node).get();
    var allValidMethodCalls =
        ASTs.isInitExpr(call).flatMap(LocalVariableDeclaration::fromVariableDeclarator).stream()
            .flatMap(LocalDeclaration::findAllMethodCalls)
            .filter(mce -> matchingMethods.contains(mce.getNameAsString()))
            .toList();
    if (allValidMethodCalls.isEmpty()) {
      return SuccessOrReason.reason("Couldn't find any matching methods");
    }
    // Add executeWithTimout method to the encompassing class
    var classDecl = call.findAncestor(ClassOrInterfaceDeclaration.class);
    if (classDecl.isEmpty()) {
      return SuccessOrReason.reason("Couldn't find encompassing class");
    }
    classDecl.ifPresent(cd -> addTimeoutMethodIfMissing(cu, cd));
    for (var mce : allValidMethodCalls) {
      // Wrap it with executeWithTimeout with a default 5000 of timeout
      var newCall =
          new MethodCallExpr(
              "executeWithTimeout",
              new LambdaExpr(new NodeList<>(), mce.clone()),
              new IntegerLiteralExpr(DEFAULT_TIMEOUT));
      mce.replace(newCall);
    }
    return SuccessOrReason.success();
  }
}

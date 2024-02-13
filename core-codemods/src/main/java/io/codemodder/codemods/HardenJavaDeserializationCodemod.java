package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.ObjectInputFilters;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/** Adds gadget filtering logic to {@link java.io.ObjectInputStream}. */
@Codemod(
    id = "pixee:java/harden-java-deserialization",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenJavaDeserializationCodemod extends CompositeJavaParserChanger {

  @Inject
  public HardenJavaDeserializationCodemod(
      final VariableDeclarationDeserializationShapeChanger varDeclChanger,
      final AnonymousDeserializationShapeChanger anonymousChanger) {
    super(varDeclChanger, anonymousChanger);
  }

  /**
   * This variant is used in situations where the deserialization is done with an unnamed stack
   * variable.
   *
   * <pre>{@code
   * LOG.info("Deserialized object: {}", new ObjectInputStream(httpServletRequest.getInputStream()).readObject());
   * }</pre>
   */
  private static class AnonymousDeserializationShapeChanger
      extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    @Inject
    public AnonymousDeserializationShapeChanger(
        @SemgrepScan(ruleId = "harden-java-deserialization-anonymous") final RuleSarif sarif) {
      super(
          sarif,
          ObjectCreationExpr.class,
          RegionNodeMatcher.MATCHES_START,
          CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final ObjectCreationExpr objectCreationExpr,
        final Result result) {
      replace(objectCreationExpr)
          .withStaticMethod(ObjectInputFilters.class.getName(), "createSafeObjectInputStream")
          .withStaticImport()
          .withSameArguments();
      return true;
    }

    @Override
    public List<DependencyGAV> dependenciesRequired() {
      return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
    }
  }

  private static final class VariableDeclarationDeserializationShapeChanger
      extends SarifPluginJavaParserChanger<VariableDeclarator> {

    @Inject
    public VariableDeclarationDeserializationShapeChanger(
        @SemgrepScan(ruleId = "harden-java-deserialization") final RuleSarif sarif) {
      super(
          sarif,
          VariableDeclarator.class,
          RegionNodeMatcher.MATCHES_START,
          CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final VariableDeclarator variableDeclarator,
        final Result result) {

      Statement newStatement =
          generateFilterHardeningStatement(variableDeclarator.getNameAsExpression());
      Optional<Node> wrappedParentNode = variableDeclarator.getParentNode();
      if (wrappedParentNode.isEmpty()) {
        return false;
      }

      Node parentNode = wrappedParentNode.get();
      Class<? extends Node> parentType = parentNode.getClass();

      if (FieldDeclaration.class.equals(parentType)) {
        return false;
      }

      if (VariableDeclarationExpr.class.equals(parentType)) {
        Node variableDeclarationParent = parentNode.getParentNode().get();
        Class<? extends Node> variableDeclarationParentType = variableDeclarationParent.getClass();
        if (ExpressionStmt.class.equals(variableDeclarationParentType)) {
          ExpressionStmt expressionStmt = (ExpressionStmt) variableDeclarationParent;
          ASTTransforms.addStatementAfterStatement(expressionStmt, newStatement);
          addImportIfMissing(cu, ObjectInputFilters.class.getName());
          return true;
        }

        // if we're not in an expression statement, we might be in a try-with-resources statement
        if (TryStmt.class.equals(variableDeclarationParentType)) {
          TryStmt tryStatement = (TryStmt) variableDeclarationParent;
          BlockStmt tryBlock = tryStatement.getTryBlock();
          ASTTransforms.addStatementBeforeStatement(tryBlock.getStatements().get(0), newStatement);
          addImportIfMissing(cu, ObjectInputFilters.class.getName());
          return true;
        }
      }

      return false;
    }

    /**
     * Generates an expression to invoke {@link
     * ObjectInputFilters#enableObjectFilterIfUnprotected(ObjectInputStream)} on the original scope
     * (the {@link ObjectInputStream}).
     */
    private Statement generateFilterHardeningStatement(final Expression originalScope) {
      // this statement is the callback to our hardening code
      var callbackClass = new NameExpr(ObjectInputFilters.class.getSimpleName());
      var hardenStatement = new MethodCallExpr(callbackClass, "enableObjectFilterIfUnprotected");
      hardenStatement.addArgument(originalScope);
      return new ExpressionStmt(hardenStatement);
    }

    @Override
    public List<DependencyGAV> dependenciesRequired() {
      return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
    }
  }
}

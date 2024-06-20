package io.codemodder.codemods;

import static io.codemodder.javaparser.ASTExpectations.expect;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

final class DefaultXXEJavaDOMRemediatorStrategy implements XXEJavaDOMRemediatorStrategy {

  private final List<XXEFixer> fixers;

  DefaultXXEJavaDOMRemediatorStrategy() {
    this.fixers = List.of(new DocumentBuilderFactoryFixer());
  }

  @Override
  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {

    List<MethodCallExpr> newFactoryInstanceCalls =
        cu.findAll(MethodCallExpr.class).stream()
            .filter(m -> "newInstance".equals(m.getNameAsString()))
            .toList();
    if (newFactoryInstanceCalls.isEmpty()) {
      return CodemodFileScanningResult.none();
    }

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    List<CodemodChange> changes = new ArrayList<>();

    for (T issue : issuesForFile) {

      String findingId = getKey.apply(issue);
      int line = getLine.apply(issue);
      Integer column = getColumn.apply(issue);
      for (XXEFixer fixer : fixers) {
        FixAttempt fixAttempt = fixer.tryFix(issue, line, column, cu);
        if (!fixAttempt.isResponsibleFixer) {
          continue;
        }
        if (fixAttempt.isFixed) {
          CodemodChange change =
              CodemodChange.from(line, new FixedFinding(findingId, detectorRule));
          changes.add(change);
        } else {
          UnfixedFinding unfixedFinding =
              new UnfixedFinding(findingId, detectorRule, path, line, fixAttempt.reasonNotFixed);
          unfixedFindings.add(unfixedFinding);
        }
      }
    }

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  interface XXEFixer {
    <T> FixAttempt tryFix(T issue, int line, Integer column, CompilationUnit cu);
  }

  private record FixAttempt(boolean isResponsibleFixer, boolean isFixed, String reasonNotFixed) {

    FixAttempt {
      if (!isResponsibleFixer && isFixed) {
        throw new IllegalStateException("Cannot be fixed by a non-responsible fixer");
      }
      if (!isFixed && reasonNotFixed == null) {
        throw new IllegalStateException("Reason must be provided if not fixed");
      }
    }
  }

  /** Fixer for DocumentBuilderFactory.newInstance() calls */
  private static class DocumentBuilderFactoryFixer implements XXEFixer {

    @Override
    public <T> FixAttempt tryFix(
        final T issue, final int line, final Integer column, CompilationUnit cu) {
      List<MethodCallExpr> candidateMethods =
          cu.findAll(MethodCallExpr.class).stream()
              .filter(
                  m ->
                      m.getRange()
                          .isPresent()) // this may be true of nodes we've inserted in a previous
              // fix
              .filter(m -> m.getRange().get().begin.line == line)
              .toList();

      if (column != null) {
        Position reportedPosition = new Position(line, column);
        candidateMethods =
            candidateMethods.stream()
                .filter(m -> m.getRange().get().contains(reportedPosition))
                .toList();
      }

      candidateMethods =
          candidateMethods.stream()
              .filter(m -> "newInstance".equals(m.getNameAsString()))
              .filter(
                  m -> {
                    Optional<VariableDeclarator> newFactoryVariableRef =
                        expect(m).toBeMethodCallExpression().initializingVariable().result();
                    if (newFactoryVariableRef.isEmpty()) {
                      return false;
                    }
                    String type = newFactoryVariableRef.get().getTypeAsString();
                    return "SAXParserFactory".equals(type)
                        || type.endsWith(".SAXParserFactory")
                        || "DocumentBuilderFactory".equals(type)
                        || type.endsWith(".DocumentBuilderFactory");
                  })
              .toList();

      if (candidateMethods.isEmpty()) {
        return new FixAttempt(false, false, "No calls at that location");
      } else if (candidateMethods.size() > 1) {
        return new FixAttempt(
            false,
            false,
            "Multiple calls found at the given location and that may cause confusion");
      }

      MethodCallExpr newFactoryInstanceCall = candidateMethods.get(0);
      Optional<VariableDeclarator> newFactoryVariableRef =
          expect(newFactoryInstanceCall).toBeMethodCallExpression().initializingVariable().result();
      VariableDeclarator newFactoryVariable = newFactoryVariableRef.get();
      Optional<Statement> variableDeclarationStmtRef =
          newFactoryVariable.findAncestor(Statement.class);

      if (variableDeclarationStmtRef.isEmpty()) {
        return new FixAttempt(true, false, "Not assigned as part of statement");
      }

      Statement statement = variableDeclarationStmtRef.get();
      Optional<BlockStmt> block = ASTs.findBlockStatementFrom(statement);
      if (block.isEmpty()) {
        return new FixAttempt(true, false, "No block statement found for newFactory() call");
      }

      BlockStmt blockStmt = block.get();
      MethodCallExpr setFeatureGeneralEntities =
          new MethodCallExpr(
              newFactoryVariable.getNameAsExpression(),
              "setFeature",
              NodeList.nodeList(
                  new StringLiteralExpr("http://xml.org/sax/features/external-general-entities"),
                  new BooleanLiteralExpr(false)));
      MethodCallExpr setFeatureParameterEntities =
          new MethodCallExpr(
              newFactoryVariable.getNameAsExpression(),
              "setFeature",
              NodeList.nodeList(
                  new StringLiteralExpr("http://xml.org/sax/features/external-parameter-entities"),
                  new BooleanLiteralExpr(false)));
      List<Statement> fixStatements =
          List.of(
              new ExpressionStmt(setFeatureGeneralEntities),
              new ExpressionStmt(setFeatureParameterEntities));

      NodeList<Statement> existingStatements = blockStmt.getStatements();
      int index = existingStatements.indexOf(statement);
      existingStatements.addAll(index + 1, fixStatements);
      return new FixAttempt(true, true, null);
    }
  }
}

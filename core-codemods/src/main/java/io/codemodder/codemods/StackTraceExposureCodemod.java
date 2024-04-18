package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.codeql.CodeQLSarifJavaParserChanger;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import javax.inject.Inject;

/** Fixes issues reported under the id "java/stack-trace-exposure" */
@Codemod(
    id = "codeql:java/stack-trace-exposure",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public class StackTraceExposureCodemod extends CodeQLSarifJavaParserChanger<Expression> {

  @Inject
  public StackTraceExposureCodemod(
      @ProvidedCodeQLScan(ruleId = "java/stack-trace-exposure") final RuleSarif sarif) {
    super(sarif, Expression.class, SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Expression expression,
      final Result result) {
    // is printStackTrace to an external stream e.g. ex.printStackTrace(response.getWriter())
    if (expression instanceof MethodCallExpr
        && expression.asMethodCallExpr().getNameAsString().equals("printStackTrace")) {
      expression.asMethodCallExpr().setArguments(new NodeList<>());
      return ChangesResult.changesApplied;
    }
    // is an argument of sendError call e.g. (response.sendError(418,<expression>))
    var maybeSendErrorCall =
        ASTs.isArgumentOfMethodCall(expression)
            .filter(mce -> mce.getNameAsString().equals("sendError"));
    if (maybeSendErrorCall.isPresent()) {
      var sendErrorCall = maybeSendErrorCall.get();
      NodeList<Expression> newArguments = NodeList.nodeList(sendErrorCall.getArgument(0));
      sendErrorCall.setArguments(newArguments);
      return ChangesResult.changesApplied;
    }
    // There are more cases here since it detects calls to other types of XSS sinks, but this should
    // cover the most common usage
    return ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "stack-trace-exposure",
        "Prevent information leak of stack trace details to HTTP responses",
        "https://codeql.github.com/codeql-query-help/java/java-stack-trace-exposure/");
  }
}

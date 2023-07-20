package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.providers.sarif.codeql.CodeQLScan;
import javax.inject.Inject;

/** Fixes issues reported under the id "java/stack-trace-exposure" */
@Codemod(
    id = "codeql:java/stack-trace-exposure",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public class StackTraceExposureCodemod extends SarifPluginJavaParserChanger<Expression> {

  @Inject
  public StackTraceExposureCodemod(
      @CodeQLScan(ruleId = "java/stack-trace-exposure") final RuleSarif sarif) {
    super(sarif, Expression.class, RegionExtractor.FROM_FIRST_LOCATION);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Expression expression,
      final Result result) {
    // is printStackTrace to an external stream e.g. ex.printStackTrace(response.getWriter())
    if (expression instanceof MethodCallExpr
        && expression.asMethodCallExpr().getNameAsString().equals("printStackTrace")) {
      expression.asMethodCallExpr().setArguments(new NodeList<>());
      return true;
    }
    // is an argument of sendError call e.g. (response.sendError(418,<expression>))
    var maybeSendErrorCall =
        ASTs.isArgumentOfMethodCall(expression)
            .filter(mce -> mce.getNameAsString().equals("sendError"));
    if (maybeSendErrorCall.isPresent()) {
      var sendErrorCall = maybeSendErrorCall.get();
      NodeList<Expression> newArguments = NodeList.nodeList(sendErrorCall.getArgument(0));
      sendErrorCall.setArguments(newArguments);
      return true;
    }
    // There are more cases here since it detects calls to other types of XSS sinks, but this should
    // cover the most common usage
    return false;
  }
}

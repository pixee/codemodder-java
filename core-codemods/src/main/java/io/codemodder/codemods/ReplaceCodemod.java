package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;

import javax.inject.Inject;

/** A codemod for automatically replacing replaceAll() calls to replace()  . */
@Codemod(
        id = "sonar:java/replace-s5361",
        reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
        executionPriority = CodemodExecutionPriority.HIGH)
public class ReplaceCodemod extends SonarPluginJavaParserChanger<MethodCallExpr> {

    @Inject
    public ReplaceCodemod(
            @ProvidedSonarScan(ruleId = "java:S5361") final RuleIssues issues) {
        super(issues, MethodCallExpr.class, RegionNodeMatcher.MATCHES_START);
    }

    @Override
    public boolean onIssueFound(final CodemodInvocationContext context, final CompilationUnit cu, final MethodCallExpr node, final Issue issue) {
        String methodName = node.getNameAsString();
        if ("replaceAll".equals(methodName)) {
            node.setName("replace");
            return true;
        }
        return true;
    }
}

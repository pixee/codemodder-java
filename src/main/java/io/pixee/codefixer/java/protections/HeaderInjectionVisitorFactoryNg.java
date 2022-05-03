package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.NodePredicateFactory;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import io.pixee.security.HttpHeader;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class HeaderInjectionVisitorFactoryNg implements VisitorFactory {

    @Override
    public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(final File javaFile, final CompilationUnit cu) {

        Set<Predicate<MethodCallExpr>> predicates = Set.of(
                NodePredicateFactory.withMethodName("setHeader"),
                NodePredicateFactory.withArgumentCount(2),
                NodePredicateFactory.withScopeType(cu, "javax.servlet.http.HttpServletResponse"),
                NodePredicateFactory.withArgumentType(cu, 1, "java.lang.String"),
                NodePredicateFactory.withArgumentNodeType(1, StringLiteralExpr.class).negate(),
                NodePredicateFactory.withScreamingSnakeCaseVariableNameForArgument(1).negate()
        );

        Transformer<MethodCallExpr> transformer = new Transformer<>() {
            @Override
            public TransformationResult<MethodCallExpr> transform(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
                MethodCallExpr stripNewlinesCall = new MethodCallExpr(callbackClass, "stripNewlines");
                Expression argument = methodCallExpr.getArgument(1);
                stripNewlinesCall.setArguments(NodeList.nodeList(argument));
                methodCallExpr.setArguments(
                        NodeList.nodeList(methodCallExpr.getArgument(0), stripNewlinesCall));
                Weave weave = Weave.from(methodCallExpr.getRange().get().begin.line, stripHeaderRuleId);
                return new TransformationResult<>(Optional.empty(), weave);
            }
        };

        return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
    }

    @Override
    public String ruleId() {
        return stripHeaderRuleId;
    }

    private static final NameExpr callbackClass = new NameExpr(HttpHeader.class.getName());
    private static final String stripHeaderRuleId = "pixee:java/strip-http-header";
}

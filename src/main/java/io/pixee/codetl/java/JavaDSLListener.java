package io.pixee.codetl.java;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.MethodCallPredicateFactory;
import io.pixee.codefixer.java.ObjectCreationPredicateFactory;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.Weave;
import io.pixee.codefixer.java.protections.TransformationResult;
import io.pixee.codefixer.java.VisitorFactory;

import io.pixee.dsl.java.DSLParser;
import io.pixee.dsl.java.DSLBaseListener;
import io.pixee.codetl.java.TransformationType;
import io.pixee.codetl.java.DSLFactoryDataBased;

import java.util.List;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class JavaDSLListener extends DSLBaseListener {
	public DSLFactoryDataBased result = new DSLFactoryDataBased();
	
    @Override
    public void exitRule_id(DSLParser.Rule_idContext ctx) {
        String rule = ctx.getText();
        result.getData().setRuleId(ctx.id.getText());
   		
   		LOG.info("Rule is extracted: {}", rule);
    }
	
	@Override
	public void enterMatch(DSLParser.MatchContext ctx) {
		String matchText = ctx.getText().split("match")[1].strip();
		
		var matchReplaceCtx = ctx.match_replace();
        String matchConstructType = matchReplaceCtx.id.getText();
        Map<String, String> attributes = getAttributes(matchReplaceCtx);

        var data = result.getData();

        switch (matchConstructType) {
            case ASTNodeTypes.ConstructorCall: {
                data.setTransformationType(TransformationType.OBJECT);
                var target = attributes.get("target");
                data.add(
                        List.of(
                                cu -> ObjectCreationPredicateFactory.withArgumentCount(0),
                                cu -> ObjectCreationPredicateFactory.withType(target)));
                break;
            }
            case ASTNodeTypes.InstanceMethodCall: {
                data.setTransformationType(TransformationType.METHOD);
                var type = attributes.get("type");
                var name = attributes.get("name");

                data.add(
                        List.of(
                                cu -> MethodCallPredicateFactory.withName(name),
                                cu -> MethodCallPredicateFactory.withArgumentCount(0),
                                cu -> MethodCallPredicateFactory.withScopeType(cu, type),
                                cu -> MethodCallPredicateFactory.withScreamingSnakeCaseVariableNameForArgument(1).negate())
                );
                break;
            }
        }
		
		LOG.info("Match is extracted:{}", matchText);
	}
	
	@Override
	public void enterReplace(DSLParser.ReplaceContext ctx) {
		String matchText = ctx.getText().split("with")[1].strip();

		var matchReplaceCtx = ctx.match_replace();
        String matchConstructType = matchReplaceCtx.id.getText();
        Map<String, String> attributes = getAttributes(matchReplaceCtx);

        var data = result.getData();
        var ruleId = data.getRuleId();

        switch (matchConstructType) {
            case ASTNodeTypes.ConstructorCall: {
                var target = attributes.get("target");

                Transformer<ObjectCreationExpr, ObjectCreationExpr> transformer =
                        (objectCreationExpr, context) -> {
                            objectCreationExpr.setType(new ClassOrInterfaceType(target));
                            Weave weave =
                                    Weave.from(
                                            objectCreationExpr.getRange().get().begin.line,
                                            ruleId);
                            return new TransformationResult<>(Optional.empty(), weave);
                        };
                data.setTransformer(transformer);
                break;
            }
            case ASTNodeTypes.InstanceMethodCall: {
                var type = attributes.get("type");
                var name = attributes.get("name");

                Transformer<MethodCallExpr, MethodCallExpr> transformer =
                        (methodCallExpr, context) -> {
                            Expression readerScope = methodCallExpr.getScope().get();
                            MethodCallExpr safeExpression =
                                    new MethodCallExpr(new NameExpr(type), name);
                            safeExpression.setArguments(
                                    NodeList.nodeList(readerScope));
                            Weave weave = Weave.from(methodCallExpr.getRange().get().begin.line, ruleId);
                            methodCallExpr.getParentNode().get().replace(methodCallExpr, safeExpression);
                            return new TransformationResult<>(Optional.of(safeExpression), weave);
                        };
                data.setTransformer(transformer);
                break;
            }
        }
		
		LOG.info("Replace is extracted: {}", matchText);
	}
	
	@NotNull
    private Map<String, String> getAttributes(DSLParser.Match_replaceContext matchReplaceCtx) {
        List<String> names = matchReplaceCtx
                .name
                .stream()
                .map(Token::getText)
                .collect(Collectors.toList());

        List<String> values = matchReplaceCtx
                .value
                .stream()
                .map(RuleContext::getText)
                .collect(Collectors.toList());

        return IntStream
                .range(0, names.size())
                .boxed()
                .collect(Collectors.toMap(names::get, values::get));
    }
	
	private static final Logger LOG = LogManager.getLogger(JavaDSLListener.class);
}

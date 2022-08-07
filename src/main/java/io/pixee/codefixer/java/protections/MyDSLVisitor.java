package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.pixee.codefixer.java.ObjectCreationPredicateFactory;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import io.pixee.codetl.dsl.DSLBaseVisitor;
import io.pixee.codetl.dsl.DSLParser;
import io.pixee.codetl.java.VisitorFactoryDataBased;

import java.util.List;
import java.util.Optional;

public class MyDSLVisitor extends DSLBaseVisitor<VisitorFactory> {
  private final VisitorFactoryDataBased factory = new VisitorFactoryDataBased();

  @Override
  public VisitorFactory visitStart(DSLParser.StartContext ctx) {
    super.visitStart(ctx);

    return factory;
  }

  @Override
  public VisitorFactory visitCondition(DSLParser.ConditionContext ctx) {
    String variable = ctx.var.getText();
    String methodName = ctx.mName.getText();
    String type = ctx.type.getText();

    factory
        .getData()
        .add(
            List.of(
                ObjectCreationPredicateFactory.withArgumentCount(0),
                ObjectCreationPredicateFactory.withType(type)));

    return factory;
  }

  @Override
  public VisitorFactory visitTransformation(DSLParser.TransformationContext ctx) {
    // String variable = ctx..getText();
    String methodName = ctx.mName.getText();
    String type = ctx.type.getText();

    Transformer<ObjectCreationExpr, ObjectCreationExpr> transformer =
        (objectCreationExpr, context) -> {
          objectCreationExpr.setType(new ClassOrInterfaceType(type));
          Weave weave =
              Weave.from(
                  objectCreationExpr.getRange().get().begin.line,
                  "pixee:java/secure-random"); // TODO: rule id?
          return new TransformationResult<>(Optional.empty(), weave);
        };
    factory.getData().setCreationExpr(transformer);

    return factory;
  }
}

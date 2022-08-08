package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.pixee.codefixer.java.ObjectCreationPredicateFactory;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import io.pixee.codetl.dsl.DSLBaseVisitor;
import io.pixee.codetl.dsl.DSLParser;
import io.pixee.codetl.java.VisitorFactoryData;
import io.pixee.codetl.java.VisitorFactoryDataBased;
import org.jetbrains.annotations.NotNull;

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
  public VisitorFactory visitConstructorCall(DSLParser.ConstructorCallContext ctx) {
    String source = unquote(ctx.source.getText());
    String target = unquote(ctx.target.getText());

    VisitorFactoryData<ObjectCreationExpr> data = factory
            .getData();
    data
            .add(
                    List.of(
                            ObjectCreationPredicateFactory.withArgumentCount(0),
                            ObjectCreationPredicateFactory.withType(source)));

    Transformer<ObjectCreationExpr, ObjectCreationExpr> transformer =
            (objectCreationExpr, context) -> {
              objectCreationExpr.setType(new ClassOrInterfaceType(target));
              Weave weave =
                      Weave.from(
                              objectCreationExpr.getRange().get().begin.line,
                              "pixee:java/secure-random"); // TODO: rule id?
              return new TransformationResult<>(Optional.empty(), weave);
            };

    data.setCreationExpr(transformer);

    return factory;
  }

  @NotNull
  private String unquote(String target) {
    return target.substring(1, target.length() - 1);
  }
}

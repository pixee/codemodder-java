package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.ReviewGuidance;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;

import javax.inject.Inject;
import java.util.Optional;

/** A codemod for automatically fixing missing @Override annotations. MoveArrayDesignatorsNextToTypeCodemod*/
@Codemod(
    id = "sonar:java/move-array-designators-next-to-type-s1197",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class MoveArrayDesignatorsNextToTypeCodemod extends SonarPluginJavaParserChanger<SimpleName> {

  @Inject
  public MoveArrayDesignatorsNextToTypeCodemod(
      @ProvidedSonarScan(ruleId = "java:S1197") final RuleIssues issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName simpleName,
      final Issue issue) {


      simpleName.setRange(null);
      simpleName.setTokenRange(null);

      final Node parentNode = simpleName.getParentNode().get();

      parentNode.setRange(null);
      parentNode.setTokenRange(null);

      final NodeWithType<Node, ArrayType> parentNodeWithType = (NodeWithType<Node, ArrayType>) parentNode;

       final ArrayType arrayType = parentNodeWithType.getType();

       Type type = arrayType;
       while (type instanceof ArrayType){
           type.setRange(null);
           type.setTokenRange(null);
           type = ((ArrayType) type).getComponentType();
       }

       /*final Type baseType = getInnermostType(arrayType);

       final int arrayLevel = arrayType.getArrayLevel();

       final ArrayType newArrayType = createArrayType(baseType, arrayLevel);

       arrayType.replace(newArrayType);*/


    return true;
  }

    public static ArrayType createArrayType(Type baseType, int n) {
        ArrayType arrayType = new ArrayType(baseType);

        for (int i = 1; i < n; i++) {
            arrayType = new ArrayType(arrayType);
        }

        return arrayType;
    }

    public static Type getInnermostType(Type type) {
        while (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            type = arrayType.getComponentType();
        }
        return type;
    }
}

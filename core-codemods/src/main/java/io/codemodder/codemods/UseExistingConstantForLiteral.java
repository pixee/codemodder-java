package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.providers.sonar.api.Issue;

import java.util.List;
import java.util.Optional;

public class UseExistingConstantForLiteral extends DefineConstantForLiteral {

    UseExistingConstantForLiteral(final CodemodInvocationContext context,
                             final CompilationUnit cu,
                             final StringLiteralExpr stringLiteralExpr,
                             final Issue issue){
        super(context, cu, stringLiteralExpr, issue);
    }
  @Override
  protected String getConstantName() {
    return getDefinedConstantValueInMessage(issue.getMessage());
  }

  // TODO
    public static String getDefinedConstantValueInMessage(final String message) {
        // Find the first occurrence of single quote
        int startIndex = message.indexOf("'");
        if (startIndex != -1) {
            // Find the next occurrence of single quote starting from the position after the first quote
            int endIndex = message.indexOf("'", startIndex + 1);
            if (endIndex != -1) {
                // Extract the value between the single quotes
                return message.substring(startIndex + 1, endIndex);
            }
        }
        // Return an empty string if no single quotes were found
        return "";
    }

}

package io.codemodder.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RemoveUnusedLocalVariablesTest {

  @Test
  void it_removes_s() throws IOException {
    String code =
        """
	    class A{
		    void f(){
			    String s = "something";
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);

    ASTTransforms.removeUnusedLocalVariables(methodDecl);

    assertThat(methodDecl.findAll(VariableDeclarator.class).size(), equalTo(0));
  }

  @Test
  void it_removes_s_definite_assignment() throws IOException {
    String code =
        """
	    class A{
		    void f(){
			    String s;
			    s = "something";
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);

    ASTTransforms.removeUnusedLocalVariables(methodDecl);

    assertThat(methodDecl.findAll(VariableDeclarator.class).size(), equalTo(0));
    assertThat(methodDecl.findAll(AssignExpr.class).size(), equalTo(0));
  }
}

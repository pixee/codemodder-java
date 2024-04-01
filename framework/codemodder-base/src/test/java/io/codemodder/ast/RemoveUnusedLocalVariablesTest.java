package io.codemodder.ast;

import static io.codemodder.ast.TestUtils.parseCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import org.junit.jupiter.api.Test;

final class RemoveUnusedLocalVariablesTest {

  @Test
  void it_removes_s() {
    String code =
        """
	    class A{
		    void f(){
			    String s = "something";
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);

    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);

    ASTTransforms.removeUnusedLocalVariables(methodDecl);

    assertThat(methodDecl.findAll(VariableDeclarator.class).size(), equalTo(0));
  }

  @Test
  void it_removes_s_definite_assignment() {
    String code =
        """
	    class A{
		    void f(){
			    String s;
			    s = "something";
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);

    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);

    ASTTransforms.removeUnusedLocalVariables(methodDecl);

    assertThat(methodDecl.findAll(VariableDeclarator.class).size(), equalTo(0));
    assertThat(methodDecl.findAll(AssignExpr.class).size(), equalTo(0));
  }
}

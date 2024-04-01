package io.codemodder.ast;

import static io.codemodder.ast.TestUtils.parseCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import org.junit.jupiter.api.Test;

final class RemoveEmptyStringConcatenationTest {

  @Test
  void it_removes_right() {
    String code =
        """
	    class A{
		    void f(){
			    String s = "first" + "";
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);

    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);
    var node = cu.findAll(VariableDeclarator.class).get(0);

    ASTTransforms.removeEmptyStringConcatenation(methodDecl);

    assertThat(node.getInitializer().get().toString(), equalTo("\"first\""));
  }

  void it_removes_left() {
    String code =
        """
	    class A{
		    void f(){
			    String s = "" + "second";
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);
    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);
    var node = cu.findAll(VariableDeclarator.class).get(0);

    ASTTransforms.removeEmptyStringConcatenation(methodDecl);

    assertThat(node.getInitializer().get().toString(), equalTo("\"second\""));
  }

  void it_removes_concatenation() {
    String code =
        """
	    class A{
		    void f(){
			    String s = "" + "";
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);
    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);
    var node = cu.findAll(VariableDeclarator.class).get(0);

    ASTTransforms.removeEmptyStringConcatenation(methodDecl);

    assertThat(node.getInitializer().get().toString(), equalTo("\"\""));
  }

  void it_resolves_and_removes_left() {
    String code =
        """
	    class A{
		    void f(){
			    String a = "";
			    String s = a + "second";
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);
    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);
    var node = cu.findAll(VariableDeclarator.class).get(1);

    ASTTransforms.removeEmptyStringConcatenation(methodDecl);

    assertThat(node.getInitializer().get().toString(), equalTo("\"second\""));
  }
}

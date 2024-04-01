package io.codemodder.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RemoveEmptyStringConcatenationTest {

  @Test
  void it_removes_right() throws IOException {
    String code =
        """
	    class A{
		    void f(){
			    String s = "first" + "";
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);
    var node = cu.findAll(VariableDeclarator.class).get(0);

    ASTTransforms.removeEmptyStringConcatenation(methodDecl);

    assertThat(node.getInitializer().get().toString(), equalTo("\"first\""));
  }

  @Test
  void it_removes_left() throws IOException {
    String code =
        """
	    class A{
		    void f(){
			    String s = "" + "second";
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();
    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);
    var node = cu.findAll(VariableDeclarator.class).get(0);

    ASTTransforms.removeEmptyStringConcatenation(methodDecl);

    assertThat(node.getInitializer().get().toString(), equalTo("\"second\""));
  }

  @Test
  void it_removes_concatenation() throws IOException {
    String code =
        """
	    class A{
		    void f(){
			    String s = "" + "";
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();
    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);
    var node = cu.findAll(VariableDeclarator.class).get(0);

    ASTTransforms.removeEmptyStringConcatenation(methodDecl);

    assertThat(node.getInitializer().get().toString(), equalTo("\"\""));
  }

  @Test
  void it_resolves_and_removes_left() throws IOException {
    String code =
        """
	    class A{
		    void f(){
			    String a = "";
			    String s = a + "second";
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();
    var methodDecl = cu.findAll(MethodDeclaration.class).get(0);
    var node = cu.findAll(VariableDeclarator.class).get(1);

    ASTTransforms.removeEmptyStringConcatenation(methodDecl);

    assertThat(node.getInitializer().get().toString(), equalTo("\"second\""));
  }
}

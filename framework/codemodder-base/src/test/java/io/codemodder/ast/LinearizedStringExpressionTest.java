package io.codemodder.ast;

import static io.codemodder.ast.TestUtils.parseCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LinearizedStringExpressionTest {

  @Test
  void it_finds_all_pieces() {
    String code =
        """
	    class A{
		    void f(){
			    String a = "first";
			    String b;
			    b = "second";
			    String c = "";
			    c = "third";
			    String all = a + b + c;
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);

    var node = cu.findAll(VariableDeclarator.class).get(3);
    var linearized = new LinearizedStringExpression(node.getInitializer().get());
    var values = linearized.getLinearized().stream().map(n -> n.toString()).toList();
    assertThat(values, equalTo(List.of("\"first\"", "\"second\"", "\"third\"")));
  }

  void it_does_not_resolve_ambiguous_expressions() {
    String code =
        """
	    class A{
		    void f(boolean condition){
			    String a = "first";
			    String b;
			    if (condition){
				    b = "middle";
			    }else{
				    b = "second";
			    }
			    String c = "";
			    c = "third";
			    String all = a + b + c;
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);

    var node = cu.findAll(VariableDeclarator.class).get(3);
    var linearized = new LinearizedStringExpression(node.getInitializer().get());
    var values = linearized.getLinearized().stream().map(n -> n.toString()).toList();
    assertThat(values, equalTo(List.of("\"first\"", "b", "\"third\"")));
  }

  void it_does_not_resolve_names_that_are_not_string() {
    String code =
        """
	    class A{
		    void f(){
			    String a = "first";
			    int b  = 2;
			    String c = "";
			    c = "third";
			    String all = a + b + c;
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);

    var node = cu.findAll(VariableDeclarator.class).get(3);
    var linearized = new LinearizedStringExpression(node.getInitializer().get());
    var values = linearized.getLinearized().stream().map(n -> n.toString()).toList();
    assertThat(values, equalTo(List.of("\"first\"", "b", "\"third\"")));
  }

  void it_does_not_resolve_not_local_variables() {
    String code =
        """
	    class B{
		    public String b = "middle";
	    }

	    class A{
		    void f(){
			    String a = "first";
			    String c = "";
			    c = "third";
			    String all = a + B.b + c;
		    }
	    }
    """;
    CompilationUnit cu = parseCode(code);

    var node = cu.findAll(VariableDeclarator.class).get(3);
    var linearized = new LinearizedStringExpression(node.getInitializer().get());
    var values = linearized.getLinearized().stream().map(n -> n.toString()).toList();
    assertThat(values, equalTo(List.of("\"first\"", "b", "\"third\"")));
  }
}

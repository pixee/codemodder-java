package io.codemodder.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class LinearizedStringExpressionTest {

  @Test
  void it_finds_all_pieces() throws IOException {
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
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var node = cu.findAll(VariableDeclarator.class).get(3);
    var linearized = new LinearizedStringExpression(node.getInitializer().get());
    var values = linearized.getLinearized().stream().map(n -> n.toString()).toList();
    assertThat(values, equalTo(List.of("\"first\"", "\"second\"", "\"third\"")));
  }

  @Test
  void it_does_not_resolve_ambiguous_expressions() throws IOException {
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
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var node = cu.findAll(VariableDeclarator.class).get(3);
    var linearized = new LinearizedStringExpression(node.getInitializer().get());
    var values = linearized.getLinearized().stream().map(n -> n.toString()).toList();
    assertThat(values, equalTo(List.of("\"first\"", "b", "\"third\"")));
  }

  @Test
  void it_does_not_resolve_names_that_are_not_string() throws IOException {
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

    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var node = cu.findAll(VariableDeclarator.class).get(3);
    var linearized = new LinearizedStringExpression(node.getInitializer().get());
    var values = linearized.getLinearized().stream().map(n -> n.toString()).toList();
    assertThat(values, equalTo(List.of("\"first\"", "b", "\"third\"")));
  }

  @Test
  void it_does_not_resolve_not_local_variables() throws IOException {
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

    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var node = cu.findAll(VariableDeclarator.class).get(3);
    var linearized = new LinearizedStringExpression(node.getInitializer().get());
    var values = linearized.getLinearized().stream().map(n -> n.toString()).toList();
    assertThat(values, equalTo(List.of("\"first\"", "B.b", "\"third\"")));
  }
}

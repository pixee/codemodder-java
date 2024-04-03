package io.codemodder.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.javaparser.JavaParserFactory;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

final class MergeConcatenatedLiteralsTest {

  @Test
  void it_merge_concatenated_literals() throws IOException{
    String code =
        """
    class A{
	    void f(String a, String b){
		    String s =  a  + "3" + b + "7" + "8" + "9";
	    }
    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();
    // var name = cu.findAll(NameExpr.class).get(1);
    var node = cu.findAll(VariableDeclarator.class).get(0);
    var expression = node.getInitializer().get();
    ASTTransforms.mergeConcatenatedLiterals(expression);
    assertThat(node.getInitializer().get().toString(), equalTo("a + \"3\" + b + \"789\""));

  }

  @Test
  void it_leaves_enclosed_expressions_alone() throws IOException{
    String code =
        """
    class A{
	    void f(String a, String b){
		    String s =  ("1" + "2") + "3" + "4";
	    }
    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();
    // var name = cu.findAll(NameExpr.class).get(1);
    var node = cu.findAll(VariableDeclarator.class).get(0);
    var expression = node.getInitializer().get();
    ASTTransforms.mergeConcatenatedLiterals(expression);
    assertThat(node.getInitializer().get().toString(), equalTo("(\"12\") + \"34\""));

  }
  @Test
  void it_merges_referenced_literals_in_referenced_variable() throws IOException{
    String code =
        """
    class A{
	    void f(){
		    String a =  "1" + "2";
		    String s =  a;
	    }
    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();
    // var name = cu.findAll(NameExpr.class).get(1);
    var a = cu.findAll(VariableDeclarator.class).get(0);
    var s = cu.findAll(VariableDeclarator.class).get(1);
    ASTTransforms.mergeConcatenatedLiterals(s.getInitializer().get());
    assertThat(a.getInitializer().get().toString(), equalTo("\"12\""));

  }
}

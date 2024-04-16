package io.codemodder.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ASTTransformsTest {

  @Test
  void it_maintains_children_order() throws IOException {
    String code =
        """
	    class A{
		    void f(File f){
			    int b = 2;
			    int c = 2;
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var newStmt = StaticJavaParser.parseStatement("int a = 1;");
    var blockStmt = cu.findAll(BlockStmt.class).get(0);
    ASTTransforms.addStatementAt(blockStmt, newStmt, 0);

    assertThat(blockStmt.getChildNodes().size(), equalTo(blockStmt.getStatements().size()));

    assertThat(blockStmt.getChildNodes().get(0), equalTo(newStmt));
    assertThat(blockStmt.getStatements().get(0), equalTo(newStmt));

    for (int i = 0; i < blockStmt.getChildNodes().size(); i++) {
      assertThat(blockStmt.getChildNodes().get(i), equalTo(blockStmt.getStatements().get(i)));
    }
  }
}

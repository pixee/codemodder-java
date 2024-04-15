package io.codemodder.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

import io.codemodder.javaparser.JavaParserFactory;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MergeStackedTryStmtTest {

  @Test
  void it_merges_try_stmts() throws IOException {
    String code =
        """
	    class A{
		    void f(File f){
			    try(var fr = new FileReader(f)){
				    try(var br = new BufferedReader(fr)){
					    System.out.println(br.readLine());
				    }
			    }
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var tryStmt = cu.findAll(TryStmt.class).get(1);
    var call = cu.findAll(ExpressionStmt.class).get(0);

    var merged = ASTTransforms.mergeStackedTryStmts(tryStmt);

    assertThat(merged.isEmpty(), equalTo(false));
    assertThat(merged.get().getResources().size(), equalTo(2));
    assertThat(merged.get().getTryBlock().getStatements().isEmpty(), equalTo(false));
    assertThat(merged.get().getTryBlock().getStatements().get(0), equalTo(call));
  }

  @Test
  void it_wont_merge_because_not_stacked() throws IOException {
    String code =
        """
	    class A{
		    void f(File f){
			    try(var fr = new FileReader(f)){
				    try(var br = new BufferedReader(fr)){
					    System.out.println(br.readLine());
				    }
				    System.out.println(fr.readLine());
			    }
		    }
	    }
    """;
    CompilationUnit cu =
        JavaParserFactory.newFactory().create(List.of()).parse(code).getResult().get();

    var tryStmt = cu.findAll(TryStmt.class).get(1);

    var merged = ASTTransforms.mergeStackedTryStmts(tryStmt);

    assertThat(merged.isEmpty(), equalTo(true));
  }
}

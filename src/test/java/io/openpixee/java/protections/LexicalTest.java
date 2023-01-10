package io.openpixee.java.protections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import io.openpixee.java.ast.ASTs;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

final class LexicalTest {

  @Test
  @Disabled
  void it_works_if_adding_inside_if_preserves_indent() {
    var original =
            "class A {\n"+
            "  void foo() {\n"+
            "    if(true)\n"+
            "      ;\n"+
            "  }\n"+
            "}";
    var expected =
            "class A {\n"+
            "  void foo() {\n"+
            "    if(true){\n"+
            "        break;\n"+
            "        ;\n"+
            "      }\n"+
            "  }\n"+
            "}";
    var cu = new JavaParser().parse(original).getResult().get();
    var bstmt = new BreakStmt();
    var estmt = cu.findAll(EmptyStmt.class).get(0);
    LexicalPreservingPrinter.setup(cu);
    ASTs.addStatementBeforeStatement(estmt, bstmt);
    assertThat(LexicalPreservingPrinter.print(cu),equalTo(expected));
  }

  @Test
  void it_works_if_adding_at_block_start_preserves_indent() {
    var original =
            "class A {\n"+
            "  void foo() {\n"+
            "    ;\n"+
            "  }\n"+
            "}";
    var expected =
            "class A {\n"+
            "  void foo() {\n"+
            "    break;\n"+
            "    ;\n"+
            "  }\n"+
            "}";
    var cu = new JavaParser().parse(original).getResult().get();
    var bstmt = new BreakStmt();
    var estmt = cu.findAll(EmptyStmt.class).get(0);
    LexicalPreservingPrinter.setup(cu);
    ASTs.addStatementBeforeStatement(estmt, bstmt);
    assertThat(LexicalPreservingPrinter.print(cu),equalTo(expected));
  }
}

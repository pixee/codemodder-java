package io.openpixee.java.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class ASTsTest {

  @Test
  void it_works_when_no_imports() {
    String code = "package foo;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTTransforms.addImportIfMissing(cu, "org.acme.Widget");
    assertThat(cu.getImports().equals(List.of(toSimpleImport("org.acme.Widget"))), is(true));
  }

  @Test
  void it_works_when_new_import_should_be_first() {
    String code = "package foo;\nimport zzz;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTTransforms.addImportIfMissing(cu, "org.acme.Widget");
    assertThat(
        cu.getImports(),
        equalTo(List.of(toSimpleImport("org.acme.Widget"), toSimpleImport("zzz"))));
  }

  @Test
  void it_works_when_new_import_should_be_last() {
    String code = "package foo;\nimport aaa;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTTransforms.addImportIfMissing(cu, "org.acme.Widget");
    assertThat(
        cu.getImports(),
        equalTo(List.of(toSimpleImport("aaa"), toSimpleImport("org.acme.Widget"))));
  }

  @Test
  void it_works_when_new_import_should_be_in_the_middle() {
    String code = "package foo;\nimport aaa;\nimport zzz;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTTransforms.addImportIfMissing(cu, "org.acme.Widget");
    assertThat(
        cu.getImports(),
        equalTo(
            List.of(
                toSimpleImport("aaa"), toSimpleImport("org.acme.Widget"), toSimpleImport("zzz"))));
  }

  @Test
  void it_works_when_break_is_added_inside_if_before_empty() {
    var original =
        "class A {\n" + "  void foo() {\n" + "    if(true)\n" + "      ;\n" + "  }\n" + "}";
    var expected =
        "class A {\n"
            + "  void foo() {\n"
            + "    if(true)\n"
            + "      {\n"
            + "        break;\n"
            + "        ;\n"
            + "      }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(original).getResult().get();
    var bstmt = new BreakStmt();
    var estmt = cu.findAll(EmptyStmt.class).get(0);
    LexicalPreservingPrinter.setup(cu);
    ASTTransforms.addStatementBeforeStatement(estmt, bstmt);
    assertEqualsIgnoreSpace(LexicalPreservingPrinter.print(cu), expected);
  }

  @Test
  void it_works_when_break_is_added_inside_if_after_empty() {
    var original =
        "class A {\n" + "  void foo() {\n" + "    if(true)\n" + "      ;\n" + "  }\n" + "}";
    var expected =
        "class A {\n"
            + "  void foo() {\n"
            + "    if(true)\n"
            + "      {\n"
            + "        ;\n"
            + "        break;\n"
            + "      }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(original).getResult().get();
    var bstmt = new BreakStmt();
    var estmt = cu.findAll(EmptyStmt.class).get(0);
    LexicalPreservingPrinter.setup(cu);
    ASTTransforms.addStatementAfterStatement(estmt, bstmt);
    assertEqualsIgnoreSpace(LexicalPreservingPrinter.print(cu), expected);
  }

  @Test
  void it_works_when_break_is_added_after_empty() {
    var original = "class A {\n" + "  void foo() {\n" + "    ;\n" + "  }\n" + "}";
    var expected = "class A {\n" + "  void foo() {\n" + "    ;\n" + "    break;\n" + "  }\n" + "}";
    var cu = new JavaParser().parse(original).getResult().get();
    var bstmt = new BreakStmt();
    var estmt = cu.findAll(EmptyStmt.class).get(0);
    LexicalPreservingPrinter.setup(cu);
    ASTTransforms.addStatementAfterStatement(estmt, bstmt);
    assertEqualsIgnoreSpace(LexicalPreservingPrinter.print(cu), expected);
  }

  @Test
  void it_detects_it_is_rhs_of_assignment() {
    var code =
        "class A {\n" + "  void foo() {\n" + "    var a = null;\n" + "    a = true;" + "  }\n" + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var exp = cu.findAll(BooleanLiteralExpr.class).get(0);
    assertThat(ASTPatterns.isAssigned(exp).isPresent(), is(true));
  }

  @Test
  void it_detects_it_initializes() {
    var code = "class A {\n" + "  void foo() {\n" + "    var a = true;\n" + "  }\n" + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var exp = cu.findAll(BooleanLiteralExpr.class).get(0);
    assertThat(ASTPatterns.isInitExpr(exp).isPresent(), is(true));
  }

  @Test
  void it_detects_it_is_a_resource() {
    var code = "class A {\n" + "  void foo() {\n" + "    try(var fr = new FileReader(new File(\"./test\"))){}\n" + "  }\n" + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var exp = cu.findAll(VariableDeclarationExpr.class).get(0);
    assertThat(ASTPatterns.isResource(exp).isPresent(), is(true));
  }

  void assertEquals(Stream<String> stream, Stream<String> expected) {
    var itc = stream.iterator();
    var ite = expected.iterator();
    // trim() will make it resistant to format problems
    while (itc.hasNext() && ite.hasNext()) {
      assertThat(itc.next(), equalTo(ite.next()));
    }
  }

  void assertEqualsIgnoreSpace(String string, String expected) {
    var stream = List.of(string.split("\n")).stream();
    var expStream = List.of(expected.split("\n")).stream();
    var itc = stream.iterator();
    var ite = expStream.iterator();
    // trim() will make it resistant to format problems
    while (itc.hasNext() && ite.hasNext()) {
      assertThat(itc.next().trim(), equalTo(ite.next().trim()));
    }
  }

  private ImportDeclaration toSimpleImport(final String typeName) {
    return new ImportDeclaration(typeName, false, false);
  }
}

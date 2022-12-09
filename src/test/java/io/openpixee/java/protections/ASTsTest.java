package io.openpixee.java.protections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
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
    ASTs.addImportIfMissing(cu, "org.acme.Widget");
    assertThat(cu.getImports().equals(List.of(toSimpleImport("org.acme.Widget"))), is(true));
  }

  @Test
  void it_works_when_new_import_should_be_first() {
    String code = "package foo;\nimport zzz;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTs.addImportIfMissing(cu, "org.acme.Widget");
    assertThat(
        cu.getImports(),
        equalTo(List.of(toSimpleImport("org.acme.Widget"), toSimpleImport("zzz"))));
  }

  @Test
  void it_works_when_new_import_should_be_last() {
    String code = "package foo;\nimport aaa;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTs.addImportIfMissing(cu, "org.acme.Widget");
    assertThat(
        cu.getImports(),
        equalTo(List.of(toSimpleImport("aaa"), toSimpleImport("org.acme.Widget"))));
  }

  @Test
  void it_works_when_new_import_should_be_in_the_middle() {
    String code = "package foo;\nimport aaa;\nimport zzz;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTs.addImportIfMissing(cu, "org.acme.Widget");
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
    ASTs.addStatementBeforeStatement(estmt, bstmt);
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
    ASTs.addStatementAfterStatement(estmt, bstmt);
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
    ASTs.addStatementAfterStatement(estmt, bstmt);
    assertEqualsIgnoreSpace(LexicalPreservingPrinter.print(cu), expected);
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

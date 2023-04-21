package io.codemodder.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
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
  void it_adds_static_import_when_last() {
    String code = "package foo;\nimport aaa;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTTransforms.addStaticImportIfMissing(cu, "org.acme.Widget.doThing");
    assertThat(
        cu.getImports(),
        equalTo(List.of(toSimpleImport("aaa"), toSimpleStaticImport("org.acme.Widget.doThing"))));
  }

  @Test
  void it_adds_static_import_when_first() {
    String code = "package foo;\nimport zzz;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTTransforms.addStaticImportIfMissing(cu, "org.acme.Widget.doThing");
    assertThat(
        cu.getImports(),
        equalTo(List.of(toSimpleStaticImport("org.acme.Widget.doThing"), toSimpleImport("zzz"))));
  }

  @Test
  void it_adds_static_import_when_middle() {
    String code = "package foo;\nimport aaa;\nimport zzz;\nclass Bar {}";
    CompilationUnit cu = new JavaParser().parse(code).getResult().get();
    ASTTransforms.addStaticImportIfMissing(cu, "org.acme.Widget.doThing");
    assertThat(
        cu.getImports(),
        equalTo(
            List.of(
                toSimpleImport("aaa"),
                toSimpleStaticImport("org.acme.Widget.doThing"),
                toSimpleImport("zzz"))));
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

  @Disabled
  @Test
  void it_works_when_LexicalPreservingPrinter_prints_lambda_changes() {
    var original =
        "class A {\n"
            + "\n"
            + "  void foo() {\n"
            + "    Consumer<Integer> lambda = a -> System.out.println(a);\n"
            + "  }\n"
            + "}";
    var expected =
        "class A {\n"
            + "\n"
            + "  void foo() {\n"
            + "    Consumer<Integer> lambda = a -> {\n"
            + "      break;\n"
            + "      System.out.println(a);\n"
            + "    };\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(original).getResult().get();
    var bstmt = new BreakStmt();
    var estmt = cu.findAll(ExpressionStmt.class).get(1);
    LexicalPreservingPrinter.setup(cu);
    ASTTransforms.addStatementBeforeStatement(estmt, bstmt);
    assertEqualsIgnoreSpace(LexicalPreservingPrinter.print(cu), expected);
  }

  @Test
  void it_works_when_break_is_added_inside_lambda_body_before_print() {
    var original =
        "class A {\n"
            + "\n"
            + "  void foo() {\n"
            + "    Consumer<Integer> lambda = a -> System.out.println(a);\n"
            + "  }\n"
            + "}";
    var expected =
        "class A {\n"
            + "\n"
            + "  void foo() {\n"
            + "    Consumer<Integer> lambda = a -> {\n"
            + "      break;\n"
            + "      System.out.println(a);\n"
            + "    };\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(original).getResult().get();
    var bstmt = new BreakStmt();
    var estmt = cu.findAll(ExpressionStmt.class).get(1);
    LexicalPreservingPrinter.setup(cu);
    ASTTransforms.addStatementBeforeStatement(estmt, bstmt);
    assertEqualsIgnoreSpace(cu.toString(), expected);
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
        "class A {\n"
            + "  void foo() {\n"
            + "    var a = null;\n"
            + "    a = true;"
            + "  }\n"
            + "}";
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
    var code =
        "class A {\n"
            + "  void foo() {\n"
            + "    try(var fr = new FileReader(new File(\"./test\"))){}\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var exp = cu.findAll(VariableDeclarationExpr.class).get(0);
    assertThat(ASTPatterns.isResource(exp).isPresent(), is(true));
  }

  @Test
  void it_calculates_scope_of_resource() {
    var code =
        "class A {\n"
            + "  void foo() {\n"
            + "    try(var fr = new FileReader(new File(\"./test\")); var fr2 = new FileReader(new"
            + " File(\"./test2\"))){\n"
            + "      ;\n"
            + "      ;\n"
            + "    }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var vde = cu.findAll(VariableDeclarationExpr.class).get(0);
    var scope =
        LocalVariableDeclaration.fromVariableDeclarator(vde.getVariable(0)).get().getScope();
    assertThat(scope.getExpressions().size(), is(1));
    assertThat(scope.getStatements().size(), is(2));
  }

  @Test
  void it_calculates_scope_of_declaration() {
    var code =
        "class A {\n"
            + "  void foo() {\n"
            + "    int i=0,j=i;\n"
            + "    ;\n"
            + "    ;\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var vde = cu.findAll(VariableDeclarationExpr.class).get(0);
    var scope =
        LocalVariableDeclaration.fromVariableDeclarator(vde.getVariable(0)).get().getScope();
    assertThat(scope.getExpressions().size(), is(1));
    assertThat(scope.getStatements().size(), is(2));
  }

  @Test
  void it_calculates_scope_of_foreach_declaration() {
    var code =
        "class A {\n"
            + "  void foo() {\n"
            + "    for(int i : List.of(1,2,3)){\n"
            + "        ;\n"
            + "        ;\n"
            + "    }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var vde = cu.findAll(VariableDeclarationExpr.class).get(0);
    var scope =
        LocalVariableDeclaration.fromVariableDeclarator(vde.getVariable(0)).get().getScope();
    assertThat(scope.getExpressions().size(), is(0));
    assertThat(scope.getStatements().size(), is(2));
  }

  @Test
  void it_calculates_scope_of_for_declaration() {
    var code =
        "class A {\n"
            + "  void foo() {\n"
            + "    for(int i=0, j=1;true;i++){\n"
            + "        ;\n"
            + "        ;\n"
            + "    }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var vde = cu.findAll(VariableDeclarationExpr.class).get(0);
    var scope =
        LocalVariableDeclaration.fromVariableDeclarator(vde.getVariable(0)).get().getScope();
    assertThat(scope.getExpressions().size(), is(3));
    assertThat(scope.getStatements().size(), is(2));
  }

  @Test
  void it_wraps_declaration_into_try_resource() {
    var code =
        "class A {\n"
            + "  void foo() {\n"
            + "    ;\n"
            + "    FileReader fr = new FileReader(new File(\"./test\"));\n"
            + "    ;\n"
            + "    ;\n"
            + "  }\n"
            + "}";
    var expected =
        "class A {\n"
            + "  void foo() {\n"
            + "    ;\n"
            + "    try (FileReader fr = new FileReader(new File(\"./test\"))) {\n"
            + "      ;\n"
            + "      ;\n"
            + "    }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    LexicalPreservingPrinter.setup(cu);
    var stmt = cu.findAll(ExpressionStmt.class).get(0);
    var vde = stmt.getExpression().asVariableDeclarationExpr();
    var scope =
        LocalVariableDeclaration.fromVariableDeclarator(vde.getVariable(0)).get().getScope();
    assertThat(scope.getExpressions().size(), is(0));
    assertThat(scope.getStatements().size(), is(2));
    ASTTransforms.wrapIntoResource(stmt, vde, scope);
    assertEqualsIgnoreSpace(LexicalPreservingPrinter.print(cu), expected);
  }

  @Test
  void it_detects_expr_is_scope_in_method_call() {
    var code =
        "class A {\n"
            + "  static void bar() {}\n"
            + "  static void foo() {\n"
            + "    A.bar();\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var ne = cu.findAll(NameExpr.class).get(0);
    assertThat(ASTPatterns.isScopeInMethodCall(ne).isPresent(), is(true));
  }

  @Test
  void merging_and_combining_trystmts_is_the_identity() {
    var code =
        "class A {\n"
            + "\n"
            + "  void foo() {\n"
            + "    try (var fr = new FileReader(new File(\"./test\"))) {\n"
            + "      try (var fr2 = new FileReader(new File(\"./test\"))) {\n"
            + "        ;\n"
            + "        ;\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var ts = cu.findAll(TryStmt.class).get(1);
    ASTTransforms.combineResources(ts);
    var tsm = cu.findAll(TryStmt.class).get(0);
    ASTTransforms.splitResources(tsm, 0);
    assertEqualsIgnoreSpace(LexicalPreservingPrinter.print(cu), code);
  }

  @Test
  void it_detects_variable_is_final() {
    var code =
        "class A {\n"
            + "\n"
            + "  void foo() {\n"
            + "    final int variable = 12;\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var vd = cu.findAll(VariableDeclarationExpr.class).get(0).getVariable(0);
    var lvd = LocalVariableDeclaration.fromVariableDeclarator(vd).get();
    assertThat(ASTPatterns.isFinalOrNeverAssigned(lvd), is(true));
  }

  @Test
  void it_detects_variable_is_not_initialized_and_effectively_final() {
    var code =
        "class A {\n"
            + "\n"
            + "  void foo() {\n"
            + "    int variable;\n"
            + "    variable = 12;\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var es = cu.findAll(ExpressionStmt.class).get(0);
    var lvd =
        new ExpressionStmtVariableDeclaration(
            es,
            es.getExpression().asVariableDeclarationExpr(),
            es.getExpression().asVariableDeclarationExpr().getVariable(0));

    assertThat(ASTPatterns.isNotInitializedAndAssignedAtMostOnce(lvd), is(true));
  }

  @Test
  void it_detects_variable_is_initialized_and_effectively_final() {
    var code =
        "class A {\n" + "\n" + "  void foo() {\n" + "    int variable = 12;\n" + "  }\n" + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var vd = cu.findAll(VariableDeclarationExpr.class).get(0).getVariable(0);
    var lvd = LocalVariableDeclaration.fromVariableDeclarator(vd).get();
    assertThat(ASTPatterns.isFinalOrNeverAssigned(lvd), is(true));
  }

  @Test
  void it_detects_that_variable_is_not_effectively_final() {
    var code =
        "class A {\n"
            + "\n"
            + "  void foo() {\n"
            + "    int variable = 12;\n"
            + "    variable++;\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var vd = cu.findAll(VariableDeclarationExpr.class).get(0).getVariable(0);
    var lvd = LocalVariableDeclaration.fromVariableDeclarator(vd).get();
    assertThat(ASTPatterns.isFinalOrNeverAssigned(lvd), is(false));
  }

  @Test
  void it_finds_local_variable_declaration() {
    var code =
        "class A {\n"
            + "\n"
            + "int variable = 10;\n"
            + "  void foo() {\n"
            + "    variable = 20;\n"
            + "    int variable = 12;\n"
            + "    variable = 24;\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var ne = cu.findAll(NameExpr.class).get(1);
    var local_vd = cu.findAll(VariableDeclarator.class).get(1);
    assertThat(
        ASTPatterns.findEarliestLocalDeclarationOf(ne, ne.getName().asString())
            .get()
            .getVariableDeclarator()
            .equals(local_vd),
        is(true));
  }

  @Test
  void it_finds_local_variable_declaration_from_initializer() {
    var code =
        "class A {\n"
            + "\n"
            + "int variable = 10;\n"
            + "  void foo() {\n"
            + "    variable = 20;\n"
            + "    int variable = 12, variable2 = variable + 1;\n"
            + "    variable = 24;\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var ne = cu.findAll(NameExpr.class).get(1);
    var local_vd = cu.findAll(VariableDeclarator.class).get(1);
    assertThat(
        ASTPatterns.findEarliestLocalDeclarationOf(ne, ne.getName().asString())
            .get()
            .getVariableDeclarator()
            .equals(local_vd),
        is(true));
  }

  @Test
  void it_finds_no_local_variable() {
    var code =
        "class A {\n"
            + "\n"
            + "int variable = 10;\n"
            + "  void foo() {\n"
            + "    variable = 20;\n"
            + "    int variable=12;\n"
            + "    variable = 24;\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var ne = cu.findAll(NameExpr.class).get(0);
    assertThat(
        ASTPatterns.findEarliestLocalDeclarationOf(ne, ne.getName().asString()).isEmpty(),
        is(true));
  }

  @Test
  void it_finds_resource_declaration() {
    var code =
        "class A {\n"
            + "\n"
            + "FileReader fr;\n"
            + "  void foo() {\n"
            + "    try (FileReader fr = new FileReader(new File(\"./test\"))) {\n"
            + "      fr.read();\n"
            + "    }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var ne = cu.findAll(NameExpr.class).get(0);
    var local_vd = cu.findAll(VariableDeclarator.class).get(1);
    assertThat(
        ASTPatterns.findEarliestLocalDeclarationOf(ne, ne.getName().asString())
            .get()
            .getVariableDeclarator()
            .equals(local_vd),
        is(true));
  }

  @Test
  void it_finds_for_declaration() {
    var code =
        "class A {\n"
            + "\n"
            + "int variable =10;\n"
            + "  void foo() {\n"
            + "    for (int variable = 12;true;) {\n"
            + "      variable++;\n"
            + "    }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var ne = cu.findAll(NameExpr.class).get(0);
    var local_vd = cu.findAll(VariableDeclarator.class).get(1);
    assertThat(
        ASTPatterns.findEarliestLocalDeclarationOf(ne, ne.getName().asString())
            .get()
            .getVariableDeclarator()
            .equals(local_vd),
        is(true));
  }

  @Test
  void it_finds_for_each_declaration() {
    var code =
        "class A {\n"
            + "\n"
            + "int variable =10;\n"
            + "  void foo() {\n"
            + "    int[] variables = new int[]{1,2,3,4};\n"
            + "    for (int variable : variables) {\n"
            + "      variable++;\n"
            + "    }\n"
            + "  }\n"
            + "}";
    var cu = new JavaParser().parse(code).getResult().get();
    var ne = cu.findAll(NameExpr.class).get(1);
    var local_vd = cu.findAll(VariableDeclarator.class).get(2);
    assertThat(
        ASTPatterns.findEarliestLocalDeclarationOf(ne, ne.getName().asString())
            .get()
            .getVariableDeclarator()
            .equals(local_vd),
        is(true));
  }

  void assertEqualsIgnoreSpace(String string, String expected) {
    var stream = Stream.of(string.split("\n"));
    var expStream = Stream.of(expected.split("\n"));
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

  private ImportDeclaration toSimpleStaticImport(final String typeName) {
    return new ImportDeclaration(typeName, true, false);
  }
}

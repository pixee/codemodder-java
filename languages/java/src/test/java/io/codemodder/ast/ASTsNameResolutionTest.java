package io.codemodder.ast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

final class ASTsNameResolutionTest {

  @Test
  void it_finds_local_declaration() {
    String code =
        "class A {\n"
            + "  int field = 0;\n"
            + "  void foo() {\n"
            + "    int field = this.field;\n"
            + "    int a = field;\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(6);
    var decl = cu.findAll(ExpressionStmt.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(
        maybeFound.get() == decl.getExpression().asVariableDeclarationExpr().getVariable(0),
        is(true));
  }

  @Test
  void it_finds_try_resource_declaration() {
    String code =
        "class A {\n"
            + "  void foo() {\n"
            + "    try(FileReader r = new FileReader(new File(\"./test\"))){\n"
            + "      r.read();\n"
            + "    }\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(6);
    var decl = cu.findAll(TryStmt.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(
        maybeFound.get() == decl.getResources().get(0).asVariableDeclarationExpr().getVariable(0),
        is(true));
  }

  @Test
  void it_finds_forinit_declaration() {
    String code =
        "class A {\n"
            + "  void foo() {\n"
            + "    for(int i =0; i< 42; i++){\n"
            + "      var j = i;\n"
            + "    }\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(3);
    var decl = cu.findAll(ForStmt.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(
        maybeFound.get()
            == decl.getInitialization().get(0).asVariableDeclarationExpr().getVariable(0),
        is(true));
  }

  @Test
  void it_finds_foreach_declaration() {
    String code =
        "class A {\n"
            + "  void foo(String[] allString) {\n"
            + "    for(String s : allString){\n"
            + "      s = \"\";\n"
            + "    }\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(7);
    var decl = cu.findAll(ForEachStmt.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl.getVariableDeclarator(), is(true));
  }

  @Test
  @Disabled // Not supported by JavaParser
  void it_finds_patternexpr_variable() {
    String code =
        "class A {\n"
            + "  void foo(Object obj) {\n"
            + "    if(obj instanceof String s){\n"
            + "      s = \"\";\n"
            + "    }\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(7);
    var decl = cu.findAll(PatternExpr.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl, is(true));
  }

  @Test
  void it_finds_exception_parameter() {
    String code =
        "class A {\n"
            + "  void foo() {\n"
            + "    try{\n"
            + "    }catch(Exception e){\n"
            + "      System.out.println(e);\n"
            + "    }\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(7);
    var decl = cu.findAll(CatchClause.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl.getParameter(), is(true));
  }

  @Test
  void it_finds_lambdaexpr_parameter() {
    String code =
        "class A {\n"
            + "  int field;\n"
            + "  void foo(int field) {\n"
            + "    var lambda = a -> this.field = a;\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(7);
    var decl = cu.findAll(LambdaExpr.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl.getParameter(0), is(true));
  }

  @Test
  void it_finds_method_parameter() {
    String code =
        "class A {\n"
            + "  int field;\n"
            + "  void foo(int field) {\n"
            + "    this.field = field;\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(5);
    var decl = cu.findAll(MethodDeclaration.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl.getParameter(0), is(true));
  }

  @Test
  void it_finds_constructor_parameter() {
    String code =
        "class A {\n"
            + "  int field;\n"
            + "  A(int field) {\n"
            + "    this.field = field;\n"
            + "  };\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(5);
    var decl = cu.findAll(ConstructorDeclaration.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl.getParameter(0), is(true));
  }

  @Test
  @Disabled // Not supported by JavaParser
  void it_finds_local_records() {
    String code =
        "class A {\n"
            + "  void foo() {\n"
            + "  record local(int field) {\n"
            + "  };\n"
            + "     var a = local.field;\n"
            + "  }\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(5);
    var decl = cu.findAll(LocalRecordDeclarationStmt.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl, is(true));
  }

  @Test
  void it_finds_local_classes() {
    String code =
        "class A {\n"
            + "  void foo() {\n"
            + "  class local {\n"
            + "    final static int field;\n"
            + "  };\n"
            + "     var a = local.field;\n"
            + "  }\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(5);
    var decl = cu.findAll(LocalClassDeclarationStmt.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl.getClassDeclaration(), is(true));
  }

  @Test
  void it_finds_fields() {
    String code =
        "class A {\n"
            + "  final static int field =0;\n"
            + "  void foo() {\n"
            + "    field = 1;\n"
            + "  }\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(3);
    var decl = cu.findAll(FieldDeclaration.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl, is(true));
  }

  @Test
  void it_finds_annotations() {
    String code =
        "class A {\n" + "  @interface Anno{}\n" + "  @Anno\n" + "  void foo() {\n" + "  }\n" + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(AnnotationExpr.class).get(0).getName();
    var decl = cu.findAll(AnnotationDeclaration.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access, access.getId());
    assertThat(maybeFound.get() == decl, is(true));
  }

  @Test
  void it_finds_top_level_classes() {
    String code =
        "class A {\n"
            + "  void foo() {\n"
            + "     var a = B.field;\n"
            + "  }\n"
            + "}\n"
            + "class B {\n"
            + "  final static int field;\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(3);
    var decl = cu.findAll(ClassOrInterfaceDeclaration.class).get(1);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl, is(true));
  }

  @Test
  void it_finds_B() {
    String code =
        "class A {\n"
            + "  class B{final static int field =0;}\n"
            + "  void foo() {\n"
            + "     class Local{\n"
            + "       class InnerLocal{\n"
            + "         void bar() {\n"
            + "           var i = B.field;\n"
            + "         }\n"
            + "       }\n"
            + "     };\n"
            + "  }\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(8);
    var decl = cu.findAll(ClassOrInterfaceDeclaration.class).get(1);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl, is(true));
  }

  @Test
  void it_finds_method_type_parameter() {
    String code =
        "class A<T> {\n" + "  <T> void foo(T arg) {\n" + "    T t = arg;\n" + "  }\n" + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(5);
    var decl = cu.findAll(MethodDeclaration.class).get(0);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl.getTypeParameter(0), is(true));
  }

  @Test
  void it_finds_Local() {
    String code =
        "class A {\n"
            + "  void foo() {\n"
            + "     class Local{\n"
            + "       final static int field =0;\n"
            + "       class InnerLocal{\n"
            + "         void bar() {\n"
            + "           var i = Local.field;\n"
            + "         }\n"
            + "       }\n"
            + "     };\n"
            + "  }\n"
            + "}";
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    var access = cu.findAll(SimpleName.class).get(7);
    var decl = cu.findAll(ClassOrInterfaceDeclaration.class).get(1);
    var maybeFound = ASTs.findNonCallableSimpleNameSource(access);
    assertThat(maybeFound.get() == decl, is(true));
  }

  // TypeDeclaration: ClassOrInterfaceDeclaration, EnumDeclaration, RecordDeclaration
}

package io.codemodder.codemods;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Test;

@Metadata(
    codemodType = MoveSwitchDefaultCaseLastCodemod.class,
    testResourceDir = "move-switch-default-last",
    dependencies = {})
final class MoveSwitchDefaultCaseLastCodemodTest implements CodemodTestMixin {

  /**
   * This test ensures that JavaParser still hasn't addressed the issue we reported with switch not
   * being handled well by the {@link LexicalPreservingPrinter} <a
   * href="https://github.com/javaparser/javaparser/issues/4104">here</a>.
   *
   * <p>If the test ever fails, we'll know that the behavior has changed and we may want to update
   * our side.
   */
  @Test
  void it_doesnt_handle_switch_well() {
    String code =
        """
            class Foo {
              void foo() {
                 switch(bar) {
                    case 1:
                      break;
                    case 2:
                      break;
                    default:
                      break;
                 }
              }
            }
            """;

    String expectedCode =
        """
                class Foo {
                  void foo() {
                     switch(bar) {
                        case 1:
                          break;
                        case 2:
                          break;
                        default:
                          break;
                        case 0:
                            break;
                        }
                  }
                }
                """;
    CompilationUnit cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    SwitchStmt switchStmt = cu.findAll(SwitchStmt.class).stream().findFirst().orElseThrow();

    SwitchEntry newEntry = new SwitchEntry();
    newEntry.setLabels(NodeList.nodeList(new IntegerLiteralExpr(0)));
    newEntry.setStatements(NodeList.nodeList(new BreakStmt()));
    switchStmt.getEntries().addLast(newEntry);

    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualTo(expectedCode);
  }
}

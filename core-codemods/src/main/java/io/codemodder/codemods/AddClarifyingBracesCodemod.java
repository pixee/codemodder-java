package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import io.codemodder.*;
import io.codemodder.providers.sarif.pmd.PmdScan;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

/**
 * Adds braces in situations where the lack of braces in combination with whitespace makes it seem
 * like statements are in a different code flow.
 */
@Codemod(
    id = "pixee:java/add-clarifying-braces",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH)
public final class AddClarifyingBracesCodemod extends SarifPluginJavaParserChanger<Node> {

  @Inject
  public AddClarifyingBracesCodemod(
      @PmdScan(ruleId = "category/java/codestyle.xml/ControlStatementBraces")
          final RuleSarif sarif) {
    super(sarif, Node.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Node node,
      final Result result) {

    // handle the while case
    if (node instanceof WhileStmt) {
      return handleUnbracedStmt(new UnbracedWhileStatement((WhileStmt) node));

      // handle the if case
    } else if (node.getParentNode().isPresent()
        && node.getParentNode().get() instanceof IfStmt ifStmt) {
      if (ifStmt.getThenStmt() == node) {
        return handleUnbracedStmt(new UnbracedIfStatement(ifStmt));
        // handle the else case
      } else if (ifStmt.getElseStmt().isPresent() && ifStmt.getElseStmt().get() == node) {
        return handleUnbracedStmt(new UnbracedElseStatement(ifStmt));
      }
    }
    return false;
  }

  /** Handles the case where the {@link Node} is a while unbracedStatement. */
  private boolean handleUnbracedStmt(final UnbracedStatement stmt) {
    Node parentNode = stmt.getParentNode();
    List<Node> childNodes = parentNode.getChildNodes();
    int index = childNodes.indexOf(stmt.getStatement());
    if (index == childNodes.size() - 1) {
      // it's the last statement in the block, there's no way to get confused, we can exit
      return false;
    }
    Node nextChildNode = childNodes.get(index + 1);
    if (nextChildNode.getRange().isEmpty()) {
      return false;
    }
    Range nextChildNodeRange = nextChildNode.getRange().get();
    int nextChildNodeBeginColumn = nextChildNodeRange.begin.column;

    Range onlyInnerStatementRange = stmt.getExistingSingleStatementRange();
    int onlyInnerStatementColumn = onlyInnerStatementRange.begin.column;

    if (nextChildNodeBeginColumn >= onlyInnerStatementColumn) {
      stmt.addBraces();
      return true;
    }
    return false;
  }

  /** Represents an unbraced control flow statement (e.g., if, while, etc.). */
  private interface UnbracedStatement {
    Node getParentNode();

    Range getExistingSingleStatementRange();

    void addBraces();

    Statement getStatement();
  }

  private record UnbracedWhileStatement(WhileStmt whileStmt) implements UnbracedStatement {
    private UnbracedWhileStatement {
      Objects.requireNonNull(whileStmt);
    }

    @Override
    public Node getParentNode() {
      return whileStmt.getParentNode().get();
    }

    @Override
    public Range getExistingSingleStatementRange() {
      return whileStmt.getBody().getRange().get();
    }

    @Override
    public void addBraces() {
      whileStmt.setBody(new BlockStmt(NodeList.nodeList(whileStmt.getBody())));
    }

    @Override
    public Statement getStatement() {
      return whileStmt;
    }
  }

  private record UnbracedIfStatement(IfStmt ifStmt) implements UnbracedStatement {
    private UnbracedIfStatement {
      Objects.requireNonNull(ifStmt);
    }

    @Override
    public Node getParentNode() {
      return ifStmt.getParentNode().get();
    }

    @Override
    public Range getExistingSingleStatementRange() {
      return ifStmt.getThenStmt().getRange().get();
    }

    @Override
    public void addBraces() {
      ifStmt.setThenStmt(new BlockStmt(NodeList.nodeList(ifStmt.getThenStmt())));
    }

    @Override
    public Statement getStatement() {
      return ifStmt;
    }
  }

  private record UnbracedElseStatement(IfStmt ifStmt) implements UnbracedStatement {
    private UnbracedElseStatement {
      Objects.requireNonNull(ifStmt);
    }

    @Override
    public Node getParentNode() {
      return ifStmt.getParentNode().get();
    }

    @Override
    public Range getExistingSingleStatementRange() {
      return ifStmt.getElseStmt().get().getRange().get();
    }

    @Override
    public void addBraces() {
      ifStmt.setElseStmt(new BlockStmt(NodeList.nodeList(ifStmt.getElseStmt().get())));
    }

    @Override
    public Statement getStatement() {
      return ifStmt;
    }
  }
}

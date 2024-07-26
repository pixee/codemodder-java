package io.codemodder.remediation.xxe;

import static io.codemodder.remediation.RemediationMessages.multipleCallsFound;
import static io.codemodder.remediation.RemediationMessages.noCallsAtThatLocation;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Fixes XXEs that are reported at the XMLReader#parse() call. */
final class XMLReaderAtParseFixer implements XXEFixer {

  @Override
  public XXEFixAttempt tryFix(final int line, final Integer column, final CompilationUnit cu) {
    List<MethodCallExpr> candidateMethods =
        cu.findAll(MethodCallExpr.class).stream()
            .filter(m -> "parse".equals(m.getNameAsString()))
            .filter(m -> m.getScope().isPresent())
            .filter(m -> m.getScope().get().isNameExpr())
            .filter(
                m -> {
                  Optional<Node> sourceRef =
                      ASTs.findNonCallableSimpleNameSource(
                          m.getScope().get().asNameExpr().getName());
                  if (sourceRef.isEmpty()) {
                    return false;
                  }
                  Node source = sourceRef.get();
                  if (source instanceof NodeWithType<?, ?>) {
                    return Set.of("XMLReader", "org.xml.sax.XMLReader")
                        .contains(((NodeWithType<?, ?>) source).getTypeAsString());
                  }
                  return false;
                })
            .filter(m -> m.getRange().isPresent())
            .filter(m -> m.getRange().get().begin.line == line)
            .toList();


    if (column != null && candidateMethods.size() > 1) {
      log.debug("Candidates={}, checking if column can offer more precision", candidateMethods.size());
      Position reportedPosition = new Position(line, column);
      candidateMethods =
          candidateMethods.stream()
              .filter(m -> m.getRange().get().contains(reportedPosition))
              .toList();
    } else {
        log.debug("No column available or only one candidate, skipping column check");
    }

    if (candidateMethods.isEmpty()) {
      return new XXEFixAttempt(false, false, noCallsAtThatLocation);
    } else if (candidateMethods.size() > 1) {
      return new XXEFixAttempt(false, false, multipleCallsFound);
    }

    MethodCallExpr parseCall = candidateMethods.get(0);
    Optional<Expression> parserRef = parseCall.getScope();
    if (parserRef.isEmpty()) {
      return new XXEFixAttempt(false, false, "No scope found for parse() call");
    } else if (!parserRef.get().isNameExpr()) {
      return new XXEFixAttempt(false, false, "Scope is not a name expression");
    }
    NameExpr parser = parserRef.get().asNameExpr();
    Optional<Statement> parseStatement = parseCall.findAncestor(Statement.class);
    if (parseStatement.isEmpty()) {
      return new XXEFixAttempt(true, false, "No statement found for parse() call");
    }
    return XMLFeatures.addFeatureDisablingStatements(
        parser.asNameExpr(), parseStatement.get(), true);
  }

  private static final Logger log = LoggerFactory.getLogger(XMLReaderAtParseFixer.class);
}

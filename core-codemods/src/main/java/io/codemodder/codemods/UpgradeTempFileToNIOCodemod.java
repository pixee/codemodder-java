package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import javax.inject.Inject;

/**
 * Upgrade the {@link java.io.File#createTempFile(String, String)} method to use the NIO version
 * {@link Files#createTempFile(String, String, FileAttribute[])}.
 */
@Codemod(
    id = "pixee:java/upgrade-tempfile-to-nio",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class UpgradeTempFileToNIOCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public UpgradeTempFileToNIOCodemod(
      @SemgrepScan(ruleId = "upgrade-tempfile-to-nio") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr foundCreateTempCall,
      final Result result) {

    NodeList<Expression> newArguments = getNewArguments(foundCreateTempCall);
    NameExpr nioFiles = new NameExpr(Files.class.getSimpleName());
    MethodCallExpr nioTmpFileCall = new MethodCallExpr(nioFiles, "createTempFile");
    nioTmpFileCall.setArguments(newArguments);
    MethodCallExpr replacement = new MethodCallExpr(nioTmpFileCall, "toFile");
    replace(foundCreateTempCall).withExpression(replacement);
    addImportIfMissing(cu, Files.class);
    return true;
  }

  private NodeList<Expression> getNewArguments(final MethodCallExpr foundCreateTempCall) {
    NodeList<Expression> newArguments = new NodeList<>();
    NodeList<Expression> existingArguments = foundCreateTempCall.getArguments();
    if (existingArguments.size() == 3) {
      newArguments.add(new MethodCallExpr(existingArguments.get(2), "toPath"));
    }
    newArguments.add(existingArguments.get(0));
    newArguments.add(existingArguments.get(1));
    return newArguments;
  }
}

package io.codemodder.codemods;

import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
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
    return replace(foundCreateTempCall)
        .withStaticMethod("java.nio.file.Files", "createTempFile")
        .withNewArguments(newArguments);
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

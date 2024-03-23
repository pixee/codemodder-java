package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.ZipSecurity;
import java.util.List;
import javax.inject.Inject;

/** Adds path escaping detection to {@link java.util.zip.ZipInputStream}. */
@Codemod(
    id = "pixee:java/harden-zip-entry-paths",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenZipEntryPathsCodemod
    extends SarifPluginJavaParserChanger<VariableDeclarator> {

  @Inject
  public HardenZipEntryPathsCodemod(
      @SemgrepScan(ruleId = "harden-zip-entry-paths") final RuleSarif sarif) {
    super(sarif, VariableDeclarator.class);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator variableDeclarator,
      final Result result) {
    ObjectCreationExpr newZipInputStreamCall =
        variableDeclarator.getInitializer().get().asObjectCreationExpr();
    NameExpr callbackClass = new NameExpr(ZipSecurity.class.getSimpleName());
    final MethodCallExpr securedCall =
        new MethodCallExpr(callbackClass, "createHardenedInputStream");
    securedCall.setArguments(newZipInputStreamCall.getArguments());
    variableDeclarator.setInitializer(securedCall);
    ASTTransforms.addImportIfMissing(cu, ZipSecurity.class);
    return ChangesResult.changesApplied(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
  }
}

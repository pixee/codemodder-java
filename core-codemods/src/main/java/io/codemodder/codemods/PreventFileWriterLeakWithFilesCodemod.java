package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.io.File;
import java.io.Writer;
import javax.inject.Inject;

/**
 * Transform calls to {@link java.io.BufferedWriter#BufferedWriter(Writer)} that have anonymous
 * {@link java.io.FileWriter#FileWriter(File)} in their constructor arguments to use an NIO method
 * instead. This prevents a file descriptor leak that can occur.
 */
@Codemod(
    id = "pixee:java/prevent-filewriter-leak-with-nio",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class PreventFileWriterLeakWithFilesCodemod
    extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

  @Inject
  public PreventFileWriterLeakWithFilesCodemod(
      @SemgrepScan(ruleId = "prevent-filewriter-leak-with-nio") final RuleSarif sarif) {
    super(sarif, ObjectCreationExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr newBufferedWriterCall,
      final Result result) {

    MethodCallExpr newFilesBufferedWriterCall = new MethodCallExpr();
    newFilesBufferedWriterCall.setScope(new NameExpr("Files"));
    newFilesBufferedWriterCall.setName("newBufferedWriter");

    Node parent = newBufferedWriterCall.getParentNode().get();
    ObjectCreationExpr newFileWriterArgument =
        newBufferedWriterCall.getArguments().get(0).asObjectCreationExpr();
    Expression fileArgument = newFileWriterArgument.getArguments().get(0);
    Expression pathArgument = new MethodCallExpr(fileArgument, "toPath");
    newFilesBufferedWriterCall.setArguments(NodeList.nodeList(pathArgument));
    parent.replace(newBufferedWriterCall, newFilesBufferedWriterCall);
    addImportIfMissing(cu, "java.nio.file.Files");
    return true;
  }
}

package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.providers.sarif.semgrep.SemgrepJavaParserChanger;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.XMLInputFactorySecurity;
import java.util.List;
import javax.inject.Inject;

/** Disables external entity resolution in {@link javax.xml.stream.XMLInputFactory} use. */
@Codemod(
    id = "pixee:java/harden-xmlinputfactory",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenXMLInputFactoryCodemod
    extends SemgrepJavaParserChanger<VariableDeclarator> {

  @Inject
  public HardenXMLInputFactoryCodemod(
      @SemgrepScan(ruleId = "harden-xmlinputfactory") final RuleSarif sarif) {
    super(sarif, VariableDeclarator.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onSemgrepResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator newFactoryVariable,
      final Result result) {
    final MethodCallExpr newFactory = newFactoryVariable.getInitializer().get().asMethodCallExpr();
    final NameExpr callbackClass = new NameExpr(XMLInputFactorySecurity.class.getSimpleName());
    final MethodCallExpr wrapperExpr = new MethodCallExpr(callbackClass, "hardenFactory");
    wrapperExpr.setArguments(NodeList.nodeList(newFactory));
    newFactoryVariable.setInitializer(wrapperExpr);
    ASTTransforms.addImportIfMissing(cu, XMLInputFactorySecurity.class);
    return true;
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }
}

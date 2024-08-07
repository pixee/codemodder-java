package io.codemodder.codemods;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.XMLInputFactorySecurity;
import java.util.List;
import javax.inject.Inject;

/** Disables external entity resolution in {@link javax.xml.stream.XMLInputFactory} use. */
@Codemod(
    id = "pixee:java/harden-xmlinputfactory",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenXMLInputFactoryCodemod
    extends SarifPluginJavaParserChanger<VariableDeclarator> {

  @Inject
  public HardenXMLInputFactoryCodemod(
      @SemgrepScan(ruleId = "harden-xmlinputfactory") final RuleSarif sarif) {
    super(sarif, VariableDeclarator.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator newFactoryVariable,
      final Result result) {
    final MethodCallExpr newFactory = newFactoryVariable.getInitializer().get().asMethodCallExpr();
    wrap(newFactory)
        .withStaticMethod(XMLInputFactorySecurity.class.getName(), "hardenFactory", true);
    return ChangesResult.changesAppliedWith(dependencies);
  }

  private static final List<DependencyGAV> dependencies =
      List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
}

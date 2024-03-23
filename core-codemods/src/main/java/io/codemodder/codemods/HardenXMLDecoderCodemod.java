package io.codemodder.codemods;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.*;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.XMLDecoderSecurity;
import java.util.List;
import javax.inject.Inject;

/** Adds gadget filtering logic to {@link java.beans.XMLDecoder} streams. */
@Codemod(
    id = "pixee:java/harden-xmldecoder-stream",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenXMLDecoderCodemod
    extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

  @Inject
  public HardenXMLDecoderCodemod(
      @SemgrepScan(ruleId = "harden-xmldecoder-stream") final RuleSarif sarif) {
    super(sarif, ObjectCreationExpr.class);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr newXmlDecoderCall,
      final Result result) {
    final Expression firstArgument = newXmlDecoderCall.getArgument(0);
    wrap(firstArgument).withStaticMethod(XMLDecoderSecurity.class.getName(), "hardenStream", true);
    return ChangesResult.changesApplied(dependencies);
  }

  private final List<DependencyGAV> dependencies = List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
}

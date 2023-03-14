package io.codemodder.codemods;

import com.contrastsecurity.sarif.Region;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.ChangeConstructorTypeVisitor;
import io.codemodder.Codemod;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.FileWeavingContext;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.providers.sarif.semgrep.SemgrepJavaParserChanger;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import javax.inject.Inject;

/** Turns {@link java.util.Random} into {@link java.security.SecureRandom}. */
@Codemod(
    id = "pixee:java/secure-random",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SecureRandomCodemod extends SemgrepJavaParserChanger {

  @Inject
  public SecureRandomCodemod(
      @SemgrepScan(pathToYaml = "/secure-random.yaml", ruleId = "secure-random")
          final RuleSarif sarif) {
    super(sarif);
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createVisitor(
      final CodemodInvocationContext context, final List<Region> regions) {
    return new ChangeConstructorTypeVisitor(
        regions, "java.security.SecureRandom", context.codemodId());
  }
}

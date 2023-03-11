package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.ChangeConstructorTypeVisitor;
import io.codemodder.ChangeContext;
import io.codemodder.CodeDirectory;
import io.codemodder.Codemod;
import io.codemodder.JavaParserChanger;
import io.codemodder.ReviewGuidance;
import io.codemodder.Sarif;
import io.codemodder.providers.sarif.semgrep.SemgrepSarifProvider;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Turns {@link java.util.Random} into {@link java.security.SecureRandom}. */
@Codemod(
    value = "pixee:java/secure-random",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SecureRandomCodemod implements JavaParserChanger {

  private final SarifSchema210 sarif;

  @Inject
  public SecureRandomCodemod(
      final CodeDirectory codeDirectory, final SemgrepSarifProvider sarifProvider)
      throws IOException, URISyntaxException {
    this.sarif = sarifProvider.getSarif(codeDirectory.asPath(), "secure-random.semgrep");
  }

  @Override
  public Optional<ModifierVisitor<ChangeContext>> createModifierVisitor(final CompilationUnit cu) {
    List<Result> results = Sarif.getResultsForCompilationUnit(sarif, cu);
    logger.debug("Found {} results in {} to change", results.size(), cu.getPrimaryTypeName().get());
    if (!results.isEmpty()) {
      return Optional.of(
          new ChangeConstructorTypeVisitor(Sarif.findRegions(results), "java.lang.SecureRandom"));
    }
    return Optional.empty();
  }

  private static final Logger logger = LoggerFactory.getLogger(SecureRandomCodemod.class);
}

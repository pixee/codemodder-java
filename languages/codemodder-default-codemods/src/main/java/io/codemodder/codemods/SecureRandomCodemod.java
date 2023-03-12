package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.ChangeConstructorTypeVisitor;
import io.codemodder.CodeDirectory;
import io.codemodder.Codemod;
import io.codemodder.FileWeavingContext;
import io.codemodder.JavaParserChanger;
import io.codemodder.ReviewGuidance;
import io.codemodder.Sarif;
import io.codemodder.providers.sarif.semgrep.SemgrepSarifProvider;
import java.io.IOException;
import java.nio.file.Path;
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
      throws IOException {
    this.sarif = sarifProvider.getSarif(codeDirectory.asPath(), "secure-random.semgrep");
  }

  @Override
  public Optional<ModifierVisitor<FileWeavingContext>> createModifierVisitor(
      final CodeDirectory codeDirectory, final Path path, final CompilationUnit cu) {
    List<Result> results = Sarif.getResultsForCompilationUnit(sarif, path);
    logger.trace("Found {} results in {} to change", results.size(), path);
    if (!results.isEmpty()) {
      return Optional.of(
          new ChangeConstructorTypeVisitor(
              Sarif.findRegions(results),
              "java.security.SecureRandom",
              "pixee:java/secure-random"));
    }
    return Optional.empty();
  }

  @Override
  public String getCodemodId() {
    return "pixee:java/secure-random";
  }

  private static final Logger logger = LoggerFactory.getLogger(SecureRandomCodemod.class);
}

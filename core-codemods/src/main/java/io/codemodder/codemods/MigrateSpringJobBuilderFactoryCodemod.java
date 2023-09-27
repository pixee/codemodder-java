package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.Optional;
import javax.inject.Inject;

/** Migrates Spring code from using JobBuilderFactory to directly using JobBuilder. */
@Codemod(
    id = "pixee:java/migrate-spring-job-builder-factory",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class MigrateSpringJobBuilderFactoryCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  private static final String RULE =
      """
          rules:
            - id: migrate-spring-job-builder-factory
              pattern: (org.springframework.batch.core.configuration.annotation.JobBuilderFactory $FACTORY).get($JOB).start($STEP).build();
              pattern-inside: |
                @EnableBatchProcessing
                class $X {
                  ...
                  @Bean
                  public Job $METHOD(Step step) {
                    ...
                  }
                  ...
                }
                  """;

  @Inject
  public MigrateSpringJobBuilderFactoryCodemod(@SemgrepScan(yaml = RULE) RuleSarif sarif) {
    super(sarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr jobBuilderFactoryBuild,
      final Result result) {

    if (!"build".equals(jobBuilderFactoryBuild.getNameAsString())) {
      return false;
    }

    Optional<MethodDeclaration> methodDeclarationRef =
        jobBuilderFactoryBuild.findAncestor(MethodDeclaration.class);
    if (methodDeclarationRef.isEmpty()) {
      // if there's no enclosing method declaration, we're in an unexpected state
      return false;
    }

    // if there's a JobRepository in the arguments to the function, use that or create a new one
    MethodDeclaration methodDeclaration = methodDeclarationRef.get();
    Parameter jobRepository;

    Optional<Parameter> jobRepositoryParameterRef =
        methodDeclaration.getParameters().stream()
            .filter(p -> p.getTypeAsString().equals(JOB_REPOSITORY) || p.getTypeAsString().equals())
            .findFirst();

    if (jobRepositoryParameterRef.isEmpty()) {
      jobRepository = jobRepositoryParameterRef.get();
    } else {
      jobRepository =
          new Parameter(
              StaticJavaParser.parseClassOrInterfaceType(JOB_REPOSITORY), "jobRepository");
      methodDeclaration.addParameter(jobRepository);
    }

    ClassOrInterfaceType jobBuilderType = StaticJavaParser.parseClassOrInterfaceType("JobBuilder");
    ObjectCreationExpr newJobBuilder = new ObjectCreationExpr();
    newJobBuilder.setType(jobBuilderType);
    newJobBuilder.addArgument(jobRepository.getNameAsString());
    newJobBuilder.addArgument(jobBuilderFactoryBuild.getArgument(0));

    addImportIfMissing(cu, JOB_REPOSITORY_FQCN);
    addImportIfMissing(cu, JOB_BUILDER_FQCN);
    removeImportIfUnused(cu, JOB_REPOSITORY_FQCN);
    return false;
  }

  private static final String JOB_REPOSITORY = "JobRepository";
  private static final String JOB_REPOSITORY_FQCN =
      "org.springframework.batch.core.repository.JobRepository";
  private static final String JOB_BUILDER_FQCN =
      "org.springframework.batch.core.job.builder.JobBuilder";
  private static final String JOB_BUILDER_FACTORY_FQCN =
      "org.springframework.batch.core.configuration.annotation.JobBuilderFactory";
}

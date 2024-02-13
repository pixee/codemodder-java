package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.ast.ASTTransforms.removeImportIfUnused;
import static io.codemodder.javaparser.ASTExpectations.expect;
import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/** Migrates Spring code from using JobBuilderFactory to directly using JobBuilder. */
@Codemod(
    id = "pixee:java/migrate-spring-job-builder-factory",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class MigrateSpringJobBuilderFactoryCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  private static final String RULE =
      """
      rules:
        - id: migrate-spring-job-builder-factory
          patterns:
            - pattern: (org.springframework.batch.core.configuration.annotation.JobBuilderFactory $FACTORY).get($JOB).start($STEP).build()
            - pattern-inside: |
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

    Optional<MethodCallExpr> previousStart =
        expect(jobBuilderFactoryBuild.getScope().get())
            .toBeMethodCallExpression()
            .withName("start")
            .result();
    if (previousStart.isEmpty()) {
      return false;
    }

    Optional<MethodCallExpr> previousGet =
        expect(previousStart.get().getScope().get())
            .toBeMethodCallExpression()
            .withName("get")
            .result();

    if (previousGet.isEmpty()) {
      return false;
    }

    Optional<FieldAccessExpr> jobBuilderFactoryName =
        expect(previousGet.get().getScope().get()).toBeFieldAccessExpression().result();
    if (jobBuilderFactoryName.isEmpty()) {
      return false;
    }

    // if there's a JobRepository in the arguments to the function, use that or create a new one
    MethodDeclaration methodDeclaration = methodDeclarationRef.get();
    Parameter jobRepository;

    Optional<Parameter> jobRepositoryParameterRef =
        methodDeclaration.getParameters().stream()
            .filter(
                p ->
                    p.getTypeAsString().equals(JOB_REPOSITORY)
                        || p.getTypeAsString().equals(JOB_REPOSITORY_FQCN))
            .findFirst();

    if (jobRepositoryParameterRef.isPresent()) {
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
    newJobBuilder.addArgument(new NameExpr(jobRepository.getNameAsString()));
    newJobBuilder.addArgument(previousGet.get().getArgument(0));

    MethodCallExpr start = new MethodCallExpr(newJobBuilder, "start");
    start.addArgument(previousStart.get().getArgument(0));

    MethodCallExpr build = new MethodCallExpr(start, "build");
    replace(jobBuilderFactoryBuild).withExpression(build);

    addImportIfMissing(cu, JOB_REPOSITORY_FQCN);
    addImportIfMissing(cu, JOB_BUILDER_FQCN);

    // remove variable if unused
    FieldAccessExpr factoryVariableName = jobBuilderFactoryName.get();
    String simpleVariableName = factoryVariableName.getNameAsString();

    // don't try to be smart about scopes or anything, just remove the variable if there's literally
    // no other references to it and it's private
    if (cu.findAll(FieldAccessExpr.class).stream()
        .noneMatch(f -> simpleVariableName.equals(f.getNameAsString()))) {
      List<VariableDeclarator> matchingVariables =
          cu.findAll(VariableDeclarator.class).stream()
              .filter(vd -> vd.getNameAsString().equals(simpleVariableName))
              .toList();
      if (matchingVariables.size() == 1) {
        VariableDeclarator variable = matchingVariables.get(0);
        Node parentNode = variable.getParentNode().get();
        if (parentNode instanceof FieldDeclaration fieldDeclaration
            && fieldDeclaration.isPrivate()) {
          fieldDeclaration.remove(variable);
          if (fieldDeclaration.getVariables().isEmpty()) {
            fieldDeclaration.remove();
          }
        }
      }
    }

    removeImportIfUnused(cu, JOB_BUILDER_FACTORY_FQCN);

    return true;
  }

  private static final String JOB_REPOSITORY_FQCN =
      "org.springframework.batch.core.repository.JobRepository";
  private static final String JOB_REPOSITORY = "JobRepository";
  private static final String JOB_BUILDER_FQCN =
      "org.springframework.batch.core.job.builder.JobBuilder";
  private static final String JOB_BUILDER_FACTORY_FQCN =
      "org.springframework.batch.core.configuration.annotation.JobBuilderFactory";
}

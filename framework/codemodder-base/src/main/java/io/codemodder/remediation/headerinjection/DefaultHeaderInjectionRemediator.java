package io.codemodder.remediation.headerinjection;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

final class DefaultHeaderInjectionRemediator implements HeaderInjectionRemediator {

  private static final Set<String> setHeaderNames = Set.of("setHeader", "addHeader");
  private final MethodDeclaration fixMethod;

  private static final String validatorMethodName = "stripNewlines";

  DefaultHeaderInjectionRemediator() {
    String fixMethodCode =
        """
            private static String stripNewlines(final String s) {
              return s.replaceAll("[\\n\\r]", "");
            }
            """;

    this.fixMethod = StaticJavaParser.parseMethodDeclaration(fixMethodCode);
  }

  @Override
  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {

    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMatcher(mce -> setHeaderNames.contains(mce.getNameAsString()))
            .withMatcher(mce -> mce.getScope().isPresent())
            .withMatcher(mce -> mce.getArguments().size() == 2)
            .withMatcher(mce -> !mce.getArgument(1).isStringLiteralExpr())
            .build();

    FixCandidateSearchResults<T> results =
        searcher.search(cu, path, detectorRule, issuesForFile, getKey, getLine, getColumn);

    List<CodemodChange> changes = new ArrayList<>();
    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      List<T> issues = fixCandidate.issues();

      MethodCallExpr setHeaderCall = fixCandidate.methodCall();
      Expression headerValueArgument = setHeaderCall.getArgument(1);
      wrap(headerValueArgument).withScopelessMethod(validatorMethodName);

      // add the validation method if it's not already present
      ClassOrInterfaceDeclaration parentClass =
          setHeaderCall.findAncestor(ClassOrInterfaceDeclaration.class).get();
      if (parentClass.isInterface()) {
        MethodCallExpr inlinedStripCall =
            new MethodCallExpr(
                headerValueArgument,
                "replaceAll",
                NodeList.nodeList(new StringLiteralExpr("[\\n\\r]"), new StringLiteralExpr("")));
        setHeaderCall.getArguments().set(1, inlinedStripCall);
      } else {
        boolean alreadyHasResourceValidationCallPresent =
            parentClass.findAll(MethodDeclaration.class).stream()
                .anyMatch(
                    md ->
                        md.getNameAsString().equals(validatorMethodName)
                            && md.getParameters().size() == 1
                            && md.getParameters().get(0).getTypeAsString().equals("String"));

        if (!alreadyHasResourceValidationCallPresent) {
          parentClass.addMember(fixMethod);
        }
      }

      // all the line numbers should be the same, so we just grab the first one
      int line = getLine.apply(fixCandidate.issues().get(0));
      List<FixedFinding> fixedFindings =
          issues.stream()
              .map(issue -> new FixedFinding(getKey.apply(issue), detectorRule))
              .toList();
      changes.add(CodemodChange.from(line, List.of(), fixedFindings));
    }

    return CodemodFileScanningResult.from(changes, results.unfixableFindings());
  }
}

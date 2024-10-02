package io.codemodder.remediation.javadeserialization;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.MethodOrConstructor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.javatuples.Pair;

public final class WhitelistJavaDeserializationRemediator implements JavaDeserializationRemediator {

  private static final String DEFAULT_CLASS_NAME = "WhitelistedObjectInputStream";

  public Optional<ObjectCreationExpr> findSource(MethodCallExpr call) {
    return call.getScope()
        .map(node -> node instanceof NameExpr ? (NameExpr) node : null)
        .flatMap(ne -> ASTs.findEarliestLocalVariableDeclarationOf(ne.getName()))
        .flatMap(lvd -> lvd.getDeclaration().getInitializer())
        .map(init -> init.isObjectCreationExpr() ? init.asObjectCreationExpr() : null)
        .filter(oce -> oce.getTypeAsString().equals(ObjectInputStream.class.getSimpleName()));
  }

  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final Function<T, Integer> getEndLine,
      final Function<T, Integer> getStartColumn) {

    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    // Match method calls
    // ois.readObject()
    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMatcher(MethodOrConstructor::isMethodCall)
            .build();

    var pairResult =
        searchAndFix(
            searcher,
            (cunit, moc) -> matchAndFix(moc),
            cu,
            path,
            detectorRule,
            issuesForFile,
            getKey,
            getStartLine,
            getEndLine,
            getStartColumn);
    changes.addAll(pairResult.getValue0());
    unfixedFindings.addAll(pairResult.getValue1());

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  private boolean matchAndFix(final MethodOrConstructor moc) {
    return Optional.of(moc)
        .map(m -> m.isMethodCall() ? (MethodCallExpr) m.asNode() : null)
        .flatMap(this::findSource)
        .filter(this::harden)
        .isPresent();
  }

  private <T> Pair<List<CodemodChange>, List<UnfixedFinding>> searchAndFix(
      final FixCandidateSearcher<T> searcher,
      final BiPredicate<CompilationUnit, MethodOrConstructor> fixer,
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final Function<T, Integer> getEndLine,
      final Function<T, Integer> getStartColumn) {
    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    FixCandidateSearchResults<T> results =
        searcher.search(
            cu,
            path,
            detectorRule,
            issuesForFile,
            getKey,
            getStartLine,
            getEndLine,
            getStartColumn);

    for (FixCandidate<T> candidate : results.fixCandidates()) {
      MethodOrConstructor call = candidate.call();
      List<T> issues = candidate.issues();
      if (fixer.test(cu, call)) {
        List<FixedFinding> fixedFindings =
            issues.stream()
                .map(issue -> new FixedFinding(getKey.apply(issue), detectorRule))
                .toList();
        CodemodChange change =
            CodemodChange.from(getStartLine.apply(issues.get(0)), List.of(), fixedFindings);
        changes.add(change);
      } else {
        issues.forEach(
            issue -> {
              final String id = getKey.apply(issue);
              final UnfixedFinding unfixableFinding =
                  new UnfixedFinding(
                      id,
                      detectorRule,
                      path,
                      getStartLine.apply(issues.get(0)),
                      "State changing effects possible or unrecognized code shape");
              unfixedFindings.add(unfixableFinding);
            });
      }
    }
    return Pair.with(changes, unfixedFindings);
  }

  private static String generateFilterClassName(final ClassOrInterfaceDeclaration classDecl) {
    var innerClassesName =
        classDecl.getMembers().stream()
            .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
            .map(BodyDeclaration::asClassOrInterfaceDeclaration)
            .map(NodeWithSimpleName::getNameAsString)
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));
    if (innerClassesName.isEmpty()) {
      return DEFAULT_CLASS_NAME;
    }
    String number =
        innerClassesName.get(innerClassesName.size() - 1).substring(DEFAULT_CLASS_NAME.length());
    if (number.isBlank()) {
      return DEFAULT_CLASS_NAME + "_1";
    }
    int num = 0;
    try {
      num = Integer.parseInt(number.substring(1)) + 1;
    } catch (NumberFormatException e) {
      return DEFAULT_CLASS_NAME;
    }
    return DEFAULT_CLASS_NAME + "_" + num;
  }

  private static void addInnerClass(
      final ClassOrInterfaceDeclaration classDecl, final String newClassName) {
    final String klass =
        """
    static class %s extends ObjectInputStream {

        public %s(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException {

            List<String> whitelist = List.of();

            if (!whitelist.contains(osc.getName())) {
                throw new SecurityException("Unauthorized deserialization of %s".formatted(osc.getName()));
            }

            return super.resolveClass(osc);
        }
    }
    """
            .formatted(newClassName, newClassName, "%s");
    classDecl.addMember(StaticJavaParser.parseBodyDeclaration(klass));
  }

  private boolean harden(final ObjectCreationExpr oce) {
    var maybeClassDecl = oce.findAncestor(ClassOrInterfaceDeclaration.class);
    if (maybeClassDecl.isPresent()) {
      var newClassName = generateFilterClassName(maybeClassDecl.get());
      addInnerClass(maybeClassDecl.get(), newClassName);
      // Change type
      oce.setType(newClassName);
      // Add imports
      oce.findCompilationUnit()
          .ifPresent(cu -> ASTTransforms.addImportIfMissing(cu, ObjectStreamClass.class));
      oce.findCompilationUnit()
          .ifPresent(cu -> ASTTransforms.addImportIfMissing(cu, IOException.class));
      oce.findCompilationUnit()
          .ifPresent(cu -> ASTTransforms.addImportIfMissing(cu, ClassNotFoundException.class));
      oce.findCompilationUnit().ifPresent(cu -> ASTTransforms.addImportIfMissing(cu, List.class));
      oce.findCompilationUnit()
          .ifPresent(cu -> ASTTransforms.addImportIfMissing(cu, SecurityException.class));
      return true;
    }
    return false;
  }
}

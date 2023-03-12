package io.codemodder;

import static io.codemodder.JavaParserUtils.addImportIfMissing;
import static io.codemodder.JavaParserUtils.regionMatchesNode;

import com.contrastsecurity.sarif.Region;
import com.github.javaparser.Position;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import java.util.List;
import java.util.Objects;

/**
 * A utility type that makes switching one type's constructor for another, with no changes to
 * arguments.
 */
public final class ChangeConstructorTypeVisitor extends ModifierVisitor<FileWeavingContext> {

  private final List<Region> regions;
  private final String newType;
  private final String newTypeSimpleName;
  private final String codemodId;

  public ChangeConstructorTypeVisitor(
      final List<Region> regions, final String newType, final String codemodId) {
    this.regions = Objects.requireNonNull(regions);
    this.newType = Objects.requireNonNull(newType);
    this.newTypeSimpleName = toSimpleName(newType);
    this.codemodId = Objects.requireNonNull(codemodId);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public Visitable visit(
      final ObjectCreationExpr objectCreationExpr, final FileWeavingContext context) {
    if (regions.stream().anyMatch(region -> regionMatchesNode(objectCreationExpr, region))) {
      if (context.isLineIncluded(objectCreationExpr.getRange().get().begin.line)) {
        objectCreationExpr.setType(new ClassOrInterfaceType(newTypeSimpleName));
        addImportIfMissing(objectCreationExpr.findCompilationUnit().get(), newType);
        Position begin = objectCreationExpr.getRange().get().begin;
        context.addWeave(Weave.from(begin.line, codemodId));
      }
    }
    return super.visit(objectCreationExpr, context);
  }

  /** Convert FQCN to simple type. */
  private String toSimpleName(final String newType) {
    return newType.substring(newType.lastIndexOf(".") + 1);
  }
}

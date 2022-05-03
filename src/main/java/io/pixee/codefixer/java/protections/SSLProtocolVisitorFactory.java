package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

/**
 * This visitor prevents the use of SSL protocols that are considered unsafe by modern standards.
 */
public final class SSLProtocolVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    return new SSLProtocolVisitor(cu);
  }

    @Override
    public String ruleId() {
        return tlsVersionUpgradeRuleId;
    }

    private static class SSLProtocolVisitor extends ModifierVisitor<FileWeavingContext> {

    private final CompilationUnit cu;

    private SSLProtocolVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final MethodCallExpr n, final FileWeavingContext context) {
      if (!context.isLineIncluded(n)) {
        return super.visit(n, context);
      }
      if ("getInstance".equals(n.getNameAsString()) && n.getScope().isPresent()) {
        TypeLocator resolver = TypeLocator.createDefault(cu);
        Expression scope = n.getScope().get();
        String scopeType = resolver.locateType(scope);
        if ("SSLContext".equals(scopeType) || "javax.net.ssl.SSLContext".equals(scopeType)) {
          if (n.getArguments().size() == 1) {
            inspectSSLContextGetInstance(n, context);
          }
        }
      } else if ("setEnabledProtocols".equals(n.getNameAsString()) && n.getScope().isPresent()) {
        TypeLocator resolver = TypeLocator.createDefault(cu);
        Expression scope = n.getScope().get();
        String scopeType = resolver.locateType(scope);
        if ("SSLSocket".equals(scopeType)
            || "javax.net.ssl.SSLSocket".equals(scopeType)
            || "SSLEngine".equals(scopeType)
            || "javax.net.ssl.SSLEngine".equals(scopeType)) {
          if (n.getArguments().size() == 1) {
            inspectSSLProtocolArraySet(n, context);
          }
        }
      } else if ("setProtocols".equals(n.getNameAsString()) && n.getScope().isPresent()) {
        TypeLocator resolver = TypeLocator.createDefault(cu);
        Expression scope = n.getScope().get();
        String scopeType = resolver.locateType(scope);
        if ("SSLParameters".equals(scopeType) || "javax.net.ssl.SSLParameters".equals(scopeType)) {
          if (n.getArguments().size() == 1) {
            inspectSSLProtocolArraySet(n, context);
          }
        }
      }
      return super.visit(n, context);
    }

    private void inspectSSLProtocolArraySet(
        final MethodCallExpr n, final FileWeavingContext context) {
      Expression argument = n.getArgument(0);
      if (argument.isArrayCreationExpr()) {
        if (isUnsafeArrayArgument(argument.asArrayCreationExpr())) {
          replaceWithSafeArgumentArray(n, context);
        }
      } else if (argument.isNameExpr()) {
        if (isUnsafeVariable(argument.asNameExpr(), true)) {
          replaceWithSafeArgumentArray(n, context);
        }
      }
    }

    private void inspectSSLContextGetInstance(
        final MethodCallExpr n, final FileWeavingContext context) {
      Expression argument = n.getArgument(0);
      if (argument.isLiteralExpr()) {
        if (isUnsafeArgument(argument.asStringLiteralExpr())) {
          replaceWithSafeArgument(n, context);
        }
      } else if (argument.isNameExpr()) {
        if (isUnsafeVariable(argument.asNameExpr(), false)) {
          replaceWithSafeArgument(n, context);
        }
      }
    }

    private boolean isUnsafeVariable(final NameExpr variableName, boolean asArray) {
      ClassOrInterfaceDeclaration holdingType = ASTs.findTypeFrom(variableName);
      if (holdingType != null) {
        Optional<FieldDeclaration> field =
            holdingType.getFieldByName(variableName.getNameAsString());
        if (field.isPresent()) {
          FieldDeclaration fieldDeclaration = field.get();
          NodeList<VariableDeclarator> variables = fieldDeclaration.getVariables();
          // if it's more than 1, i'm not sure what to do...
          if (variables.size() == 1) {
            VariableDeclarator variableDeclarator = variables.get(0);
            final Type stringType1;
            final Type stringType2;
            if (asArray) {
              stringType1 = new ArrayType(new ClassOrInterfaceType("java.lang.String"));
              stringType2 = new ArrayType(new ClassOrInterfaceType("String"));
            } else {
              stringType1 = new ClassOrInterfaceType("java.lang.String");
              stringType2 = new ClassOrInterfaceType("String");
            }
            Type variableType = variableDeclarator.getType();
            if (stringType1.equals(variableType) || stringType2.equals(variableType)) {
              if (variableName.toString().equals(variableDeclarator.getName().toString())) {
                if (variableDeclarator.getInitializer().isPresent()) {
                  Expression variableExpression = variableDeclarator.getInitializer().get();
                  if (asArray) {
                    return variableExpression.isArrayCreationExpr()
                        && isUnsafeArrayArgument(variableExpression.asArrayCreationExpr());
                  } else {
                    return variableExpression.isStringLiteralExpr()
                        && isUnsafeArgument(variableExpression.asStringLiteralExpr());
                  }
                }
              }
            }
          }
        }
      }
      return false;
    }

    private void replaceWithSafeArgument(final MethodCallExpr n, final FileWeavingContext context) {
      final StringLiteralExpr safeArgument = new StringLiteralExpr(safeTlsVersion);
      n.setArguments(NodeList.nodeList(safeArgument));
      context.addWeave(Weave.from(n.getRange().get().begin.line, tlsVersionUpgradeRuleId));
    }

    private void replaceWithSafeArgumentArray(
        final MethodCallExpr n, final FileWeavingContext context) {
      final ArrayCreationExpr safeArgument =
          new ArrayCreationExpr(new ClassOrInterfaceType("String"));
      safeArgument.setLevels(NodeList.nodeList(new ArrayCreationLevel()));
      safeArgument.setInitializer(
          new ArrayInitializerExpr(NodeList.nodeList(new StringLiteralExpr(safeTlsVersion))));
      n.setArguments(NodeList.nodeList(safeArgument));
      context.addWeave(Weave.from(n.getRange().get().begin.line, tlsVersionUpgradeRuleId));
    }

    private boolean isUnsafeArgument(final StringLiteralExpr argument) {
      return !safeTlsVersion.equals(argument.asString());
    }

    private boolean isUnsafeArrayArgument(final ArrayCreationExpr arrayCreationExpr) {
      if (arrayCreationExpr.getInitializer().isPresent()) {
        NodeList<Expression> values = arrayCreationExpr.getInitializer().get().getValues();
        if (values.size() == 0) {
          // don't know what to do here -- probably unsafe?
          return true;
        } else if (values.size() > 1) {
          // should only be one -- the safe one
          return true;
        }
        // ok, we've confirmed it's one, let's make sure it's the safe one
        Expression protocol = values.get(0);
        if (protocol.isStringLiteralExpr()) {
          return isUnsafeArgument(protocol.asStringLiteralExpr());
        } else if (protocol.isNameExpr()) {
          return isUnsafeVariable(protocol.asNameExpr(), false);
        }
      }
      return true;
    }
  }

  private static final String tlsVersionUpgradeRuleId = "tls-version-upgrade";
  private static final String safeTlsVersion = "TLSv1.2";
}

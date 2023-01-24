package io.openpixee.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import io.openpixee.java.ast.ASTs;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;

/**
 * A type that implements a time-bound search that only resolves {@link ObjectCreationExpr} or
 * {@link NameExpr}.
 */
final class TimeBoxedTypeLocator implements TypeLocator {

  private final ExecutorService executorService;

  TimeBoxedTypeLocator() {
    executorService = Executors.newCachedThreadPool();
  }

  @Override
  public String locateType(final Expression expr) {
    String name = null;
    try {
      if (expr instanceof ObjectCreationExpr) {
        name =
            runWithTimeBound(
                () -> locateTypeFromObjectCreationExpression((ObjectCreationExpr) expr));
      } else if (expr instanceof NameExpr) {
        name = runWithTimeBound(() -> locateTypeFromNameExpression((NameExpr) expr));
      }

    } catch (Exception ignored) {
    } // this could be stack overflow, timeout exception, runtime exceptions, and other stuff that
    // we can't do anything about
    return name;
  }

  private String locateTypeFromObjectCreationExpression(
      final ObjectCreationExpr objectCreationExpr) {
    final ResolvedConstructorDeclaration resolvedConstructor = objectCreationExpr.resolve();
    return toClassName(resolvedConstructor);
  }

  @NotNull
  private String toClassName(final ResolvedConstructorDeclaration resolvedConstructor) {
    return resolvedConstructor.getPackageName() + "." + resolvedConstructor.getClassName();
  }

  private String locateTypeFromNameExpression(final NameExpr nameExpr) {
    String locatedTypeName = null;
    final ResolvedValueDeclaration valueDeclaration = nameExpr.resolve();
    locatedTypeName = locateTypeName(locatedTypeName, valueDeclaration);
    if (locatedTypeName == null) {
      Optional<MethodDeclaration> methodBodyRef = ASTs.findMethodBodyFrom(nameExpr);
      if (methodBodyRef.isEmpty()) {
        return null;
      }
      MethodDeclaration methodBody = methodBodyRef.get();
      // search for variables declared within the method body
      Optional<VariableDeclarator> variableOfSameName =
          methodBody.findAll(VariableDeclarationExpr.class).stream()
              .map(VariableDeclarationExpr::getVariables)
              .flatMap(Collection::stream)
              .filter(
                  variableDeclarator ->
                      variableDeclarator.getNameAsString().equals(nameExpr.getNameAsString()))
              .findFirst();
      if (variableOfSameName.isEmpty()) {
        Optional<Parameter> parameterDeclaration =
            methodBody.getParameterByName(nameExpr.getNameAsString());
        if (parameterDeclaration.isPresent()) {
          Parameter parameter = parameterDeclaration.get();
          Type type = parameter.getType();
          if (type.isClassOrInterfaceType()) {
            ClassOrInterfaceType variableType = type.asClassOrInterfaceType();
            CompilationUnit cu = nameExpr.findCompilationUnit().get();
            for (ImportDeclaration i : cu.getImports()) {
              final Name name = i.getName();
              if (name.getIdentifier().equals(variableType.getNameAsString())) {
                return i.getNameAsString();
              }
            }
          }
        }
      } else {
        Type type = variableOfSameName.get().getType();
        if (type.isClassOrInterfaceType()) {
          ClassOrInterfaceType variableType = type.asClassOrInterfaceType();
          CompilationUnit cu = nameExpr.findCompilationUnit().get();
          for (ImportDeclaration i : cu.getImports()) {
            final Name name = i.getName();
            if (name.getIdentifier().equals(variableType.getNameAsString())) {
              return i.getNameAsString();
            }
          }
        }
      }
    }
    return locatedTypeName;
  }

  private String locateTypeName(String locatedTypeName, ResolvedValueDeclaration valueDeclaration) {
    try {
      final ResolvedType valueType = valueDeclaration.getType();
      if (valueType instanceof ResolvedReferenceType) {
        final ResolvedReferenceType referenceType = (ResolvedReferenceType) valueType;
        final Optional<ResolvedReferenceTypeDeclaration> typeDeclaration =
            referenceType.getTypeDeclaration();
        if (typeDeclaration.isPresent()) {
          final ResolvedReferenceTypeDeclaration typeDef = typeDeclaration.get();
          final String parameterPackageName = typeDef.getPackageName();
          final String parameterTypeName = typeDef.getClassName();
          locatedTypeName =
              parameterPackageName != null
                  ? String.format("%s.%s", parameterPackageName, parameterTypeName)
                  : parameterTypeName;
        }
      }
    } catch (UnsolvedSymbolException ignored) {
      // this will certainly happen due to missing understanding of the classpath
    }
    return locatedTypeName;
  }

  private <T> T runWithTimeBound(final Callable<T> callable) throws Exception {
    final Future<T> future = executorService.submit(callable);
    try {
      return future.get(timeout, timeUnit);
    } catch (TimeoutException e) {
      // remove this if you do not want to cancel the job in progress
      // or set the argument to 'false' if you do not want to interrupt the thread
      future.cancel(true);
      throw e;
    } catch (ExecutionException e) {
      // unwrap the root cause
      Throwable t = e.getCause();
      if (t instanceof Error) {
        throw (Error) t;
      } else if (t instanceof Exception) {
        throw (Exception) t;
      } else {
        throw new IllegalStateException(t);
      }
    }
  }

  private static final long timeout = 5000;
  private static final TimeUnit timeUnit = TimeUnit.MILLISECONDS;
}

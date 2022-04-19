package io.pixee.codefixer.java;

import static java.util.Objects.requireNonNull;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Helps resolve the type name of a source code expression. */
public interface TypeLocator {

  /**
   * Returns the dot-format class name for the given expression. Should handle cases like:
   *
   * <p>getClass().getPackage(); // if called on getClass(), should return "java.lang.Class"
   *
   * <p>"foo" // should return "java.lang.String"
   */
  @Nullable
  String locateType(Expression expression);

  static TypeLocator createDefault(final CompilationUnit cu) {
    return new TimedLocator(
        new CachingTypeLocator(
            new ChainingTypeLocator(
                List.of(
                    new SimpleTypeLocator(),
                    new TimeBoxedTypeLocator(),
                    new ImportBasedTypeLocator(cu.getImports()),
                    new HardcodedTypeLocator("unknown_type")))));
  }

  /**
   * This type resolver just looks at the expression of code itself and can shortcut the expensive
   * resolution process it might otherwise have to make.
   */
  final class SimpleTypeLocator implements TypeLocator {

    private final Map<String, String> tokenToTypeMap;

    SimpleTypeLocator() {
      this.tokenToTypeMap =
          Map.of(
              "false",
              "boolean",
              "true",
              "boolean",
              "Integer",
              "java.lang.Integer",
              "String",
              "java.lang.String",
              "System",
              "java.lang.System",
              "System.out",
              "java.io.PrintStream",
              "System.err",
              "java.io.PrintStream",
              "Thread.currentThread()",
              "java.lang.Thread");
    }

    @Override
    public String locateType(final Expression expression) {
      final String expressionString = expression.toString().trim();
      if (expressionString.endsWith("\"")) {
        return "java.lang.String";
      } else if (expressionString.endsWith(".getClass()")) {
        return "java.lang.Class";
      } else if (expressionString.endsWith(".toString()")) {
        return "java.lang.String";
      }
      return tokenToTypeMap.get(expressionString);
    }
  }

  /**
   * A caching locator that remembers previous expressions and their types for instantaneous
   * resolution.
   */
  final class CachingTypeLocator implements TypeLocator {

    private final TypeLocator locator;
    private final Map<Expression, String> expressionCache;

    CachingTypeLocator(final TypeLocator locator) {
      this.locator = requireNonNull(locator);
      this.expressionCache = new HashMap<>();
    }

    @Override
    public String locateType(final Expression expr) {
      String type = expressionCache.get(expr);
      if (type == null) {
        type = locator.locateType(expr);
        if (type != null) {
          expressionCache.put(expr, type);
        }
      }
      return type;
    }
  }

  /** Wrap the location with timing messages. */
  final class TimedLocator implements TypeLocator {

    private final TypeLocator wrappedResolver;

    TimedLocator(TypeLocator wrappedLocator) {
      this.wrappedResolver = Objects.requireNonNull(wrappedLocator, "wrappedLocator");
    }

    @Override
    public String locateType(Expression expression) {
      final StopWatch watch = new StopWatch();
      watch.start();
      final String resolvedTypeName = wrappedResolver.locateType(expression);
      LOG.debug(
          "Resolved {} to {} in {}ms",
          expression.toString(),
          resolvedTypeName,
          watch.getTime(TimeUnit.MILLISECONDS));
      watch.stop();
      return resolvedTypeName;
    }

    private static final Logger LOG = LogManager.getLogger(TimedLocator.class);
  }

  /**
   * Wraps a bunch of individual resolvers, and returns the first non-null. Returns null if none are
   * found.
   */
  final class ChainingTypeLocator implements TypeLocator {

    private final List<TypeLocator> locators;

    ChainingTypeLocator(final List<TypeLocator> locators) {
      this.locators = Objects.requireNonNull(locators);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Return the first non-null type name available, and if none are available, return null
     */
    @Override
    @Nullable
    public String locateType(final Expression expression) {
      for (TypeLocator locator : locators) {
        final String name = locator.locateType(expression);
        if (name != null) {
          return name;
        }
      }
      return null;
    }
  }
}

package io.codemodder;

import com.google.inject.AbstractModule;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Responsible for binding {@link Parameter}s to codemods. Also performs validation to ensure
 * provided arguments are valid.
 */
final class ParameterModule extends AbstractModule {

  private final List<ParameterArgument> parameterArguments;
  private final List<java.lang.reflect.Parameter> injectableParameters;

  ParameterModule(
      final List<ParameterArgument> codemodParameters,
      final List<java.lang.reflect.Parameter> injectableParameters) {
    this.parameterArguments = Objects.requireNonNull(codemodParameters);
    this.injectableParameters = Objects.requireNonNull(injectableParameters);
  }

  @Override
  protected void configure() {

    // this code is a little confusing to read because the collision between the
    // java.lang.reflect.Parameter and io.codemodder.Parameter
    List<java.lang.reflect.Parameter> codemodParameters =
        this.injectableParameters.stream()
            .filter(param -> param.isAnnotationPresent(CodemodParameter.class))
            .filter(param -> param.getType().equals(Parameter.class))
            .collect(Collectors.toUnmodifiableList());

    for (java.lang.reflect.Parameter param : codemodParameters) {
      CodemodParameter codemodParameter = param.getAnnotation(CodemodParameter.class);
      Codemod codemod =
          param.getDeclaringExecutable().getDeclaringClass().getAnnotation(Codemod.class);
      ParameterArgument parameterArgument =
          parameterArguments.stream()
              .filter(p -> p.codemodId().equals(codemod.id()))
              .findFirst()
              .orElse(null);

      Parameter parameter;
      if (parameterArgument != null) {
        parameter = new GivenParameter(codemodParameter, parameterArgument);
      } else {
        parameter = new NoValueProvidedParameter(codemodParameter);
      }
      String defaultValue = parameter.getDefaultValue();
      Pattern p = Pattern.compile(codemodParameter.validationPattern());
      Matcher matcher = p.matcher(defaultValue);
      if (!matcher.matches()) {
        throw new IllegalArgumentException(
            "invalid parameter default value: "
                + defaultValue
                + " for parameter: "
                + codemodParameter.name()
                + " in codemod: "
                + codemod.id()
                + " with validation pattern: "
                + codemodParameter.validationPattern());
      }

      bind(Parameter.class).annotatedWith(codemodParameter).toInstance(parameter);
    }
  }

  /**
   * This is the {@link Parameter} implementation for when no value is provided for a given
   * parameter.
   */
  private record NoValueProvidedParameter(CodemodParameter declaration) implements Parameter {

    private NoValueProvidedParameter {
      Objects.requireNonNull(declaration);
    }

    @Override
    public String getValue(final Path path, final int currentLine) {
      return declaration.defaultValue();
    }

    @Override
    public String getDescription() {
      return declaration.description();
    }

    @Override
    public String getDefaultValue() {
      return declaration.defaultValue();
    }
  }

  /**
   * This is the {@link Parameter} implementation for when a value is provided for a given
   * parameter.
   */
  private record GivenParameter(CodemodParameter declaration, ParameterArgument parameterArgument)
      implements Parameter {

    private GivenParameter {
      Objects.requireNonNull(declaration);
      Objects.requireNonNull(parameterArgument);
    }

    @Override
    public String getValue(final Path path, final int currentLine) {
      String file = parameterArgument.file();
      String line = parameterArgument.line();
      if (file == null && line == null) {
        // this is a default value for the whole run, so we return that
        return parameterArgument.value();
      }
      if (file == null || !file.equals(path.toString())) {
        // the parameter doesn't cover this file, so we return the default value
        return declaration.defaultValue();
      } else {
        // only return the value if the line isn't specified by the arg or it matches
        if (line == null || line.isEmpty() || Integer.parseInt(line) == currentLine) {
          return parameterArgument.value();
        }
      }
      // if our parameter doesn't match, we return the default value
      return declaration.defaultValue();
    }

    @Override
    public String getDescription() {
      return declaration.description();
    }

    @Override
    public String getDefaultValue() {
      if (parameterArgument.file() == null && parameterArgument.line() == null) {
        return parameterArgument.value();
      }
      return declaration.defaultValue();
    }
  }
}

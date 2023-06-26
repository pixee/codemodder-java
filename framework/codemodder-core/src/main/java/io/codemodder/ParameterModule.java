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
              .filter(p -> p.getCodemodId().equals(codemod.id()))
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

  private record GivenParameter(CodemodParameter declaration, ParameterArgument parameterArgument)
      implements Parameter {

    private GivenParameter {
      Objects.requireNonNull(declaration);
      Objects.requireNonNull(parameterArgument);
    }

    @Override
    public String getValue(final Path path, final int currentLine) {

      String file = parameterArgument.getFile();
      if (file == null || file.isEmpty() || !file.equals(path.toString())) {
        return declaration.defaultValue();
      }
      if (path.toString().equals(file)) {
        String line = parameterArgument.getLine();
        if (line == null || line.isEmpty() || Integer.parseInt(line) == currentLine) {
          return parameterArgument.getValue();
        }
      }
      return declaration.defaultValue();
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public String getDefaultValue() {
      return null;
    }
  }
}

package io.codemodder.plugins.llm;

import java.util.HashMap;
import java.util.Map;

/** Mapper that maps models to their deployment names based on environment variables. */
final class EnvironmentBasedModelMapper implements ModelMapper {

  private final HashMap<Model, String> map = new HashMap<>();

  EnvironmentBasedModelMapper() {
    this(System.getenv());
  }

  EnvironmentBasedModelMapper(final Map<String, String> environment) {
    for (final Model model : StandardModel.values()) {
      final var name = String.format(DEPLOYMENT_TEMPLATE, toEnvironmentVariableCase(model.id()));
      final var deployment = environment.getOrDefault(name, model.id());
      map.put(model, deployment);
    }
  }

  @Override
  public String getModelName(Model model) {
    return map.getOrDefault(model, model.id());
  }

  /** Converts a model ID to environment variable casing. */
  private static String toEnvironmentVariableCase(String input) {
    return input.toUpperCase().replace('-', '_').replace('.', '_');
  }

  private static final String DEPLOYMENT_TEMPLATE = "CODEMODDER_AZURE_OPENAI_%s_DEPLOYMENT";
}

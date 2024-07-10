package io.codemodder.plugins.llm;

import java.util.HashMap;

/** Mapper that maps models to their deployment names based on environment variables. */
final class EnvironmentBasedModelMapper implements ModelMapper {
  private static final String DEPLOYMENT_TEMPLATE = "CODEMODDER_AZURE_OPENAI_%s_DEPLOYMENT";

  private final HashMap<Model, String> map = new HashMap<>();

  public EnvironmentBasedModelMapper() {
    for (Model m : StandardModel.values()) {
      final var deployment = System.getenv(String.format(DEPLOYMENT_TEMPLATE, m));
      map.put(m, deployment == null ? m.id() : deployment);
    }
  }

  @Override
  public String getModelName(Model model) {
    return map.get(model);
  }
}

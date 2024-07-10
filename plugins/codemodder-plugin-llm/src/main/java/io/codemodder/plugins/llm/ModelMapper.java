package io.codemodder.plugins.llm;

/** Maps models to their deployment names. */
interface ModelMapper {
  /**
   * Maps the given model to its deployment name.
   *
   * @param model the model to map
   * @return the deployment name of the model
   */
  String getModelName(Model model);
}

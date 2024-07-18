package io.codemodder.plugins.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Unit tests for {@link EnvironmentBasedModelMapper}. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class EnvironmentBasedModelMapperTest {

  private EnvironmentBasedModelMapper mapper;

  @BeforeAll
  void before() {
    final var environment =
        Map.of(
            "CODEMODDER_AZURE_OPENAI_GPT_3_5_TURBO_0125_DEPLOYMENT",
            "my-gpt-3.5-turbo",
            "CODEMODDER_AZURE_OPENAI_GPT_4_0613_DEPLOYMENT",
            "my-gpt-4",
            "CODEMODDER_AZURE_OPENAI_GPT_4_TURBO_2024_04_09_DEPLOYMENT",
            "my-gpt-4-turbo",
            "CODEMODDER_AZURE_OPENAI_GPT_4O_2024_05_13_DEPLOYMENT",
            "my-gpt-4o");
    mapper = new EnvironmentBasedModelMapper(environment);
  }

  /** Spot checks one of the standard models to make sure the mapping works as expected */
  @Test
  void it_maps_model_name_to_deployment() {
    final var name = mapper.getModelName(StandardModel.GPT_3_5_TURBO_0125);
    assertThat(name).isEqualTo("my-gpt-3.5-turbo");
  }

  /**
   * This is a meta-test that fails when we add a new standard model but forget to update the
   * mapping in {@link #before()} to ensure that all standard models are covered.
   */
  @EnumSource(StandardModel.class)
  @ParameterizedTest
  void it_looks_up_all_standard_models(final Model model) {
    final var name = mapper.getModelName(model);
    assertThat(name).isNotEqualTo(model.id()).startsWith("my-gpt");
  }

  @Test
  void it_returns_model_id_when_no_mapping_exists() {
    // GIVEN some model that doesn't have a mapping
    final var model = mock(Model.class, withSettings().stubOnly());
    when(model.id()).thenReturn("test");
    final var name = mapper.getModelName(model);
    assertThat(name).isEqualTo(model.id());
  }
}

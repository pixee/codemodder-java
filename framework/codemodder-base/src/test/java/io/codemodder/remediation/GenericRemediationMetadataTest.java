package io.codemodder.remediation;

import static org.assertj.core.api.Assertions.assertThat;

import io.codemodder.CodemodChange;
import io.codemodder.CodemodReporterStrategy;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

final class GenericRemediationMetadataTest {

  @ParameterizedTest
  @EnumSource(GenericRemediationMetadata.class)
  void it_can_find_generic_report_elements(final GenericRemediationMetadata metadata) {
    CodemodReporterStrategy reporter = metadata.reporter();
    assertThat(reporter.getReferences()).isNotEmpty();
    assertThat(reporter.getDescription()).isNotEmpty();
    assertThat(reporter.getSummary()).isNotEmpty();
    assertThat(reporter.getChange(Path.of("Foo.java"), CodemodChange.from(5))).isNotEmpty();
  }
}

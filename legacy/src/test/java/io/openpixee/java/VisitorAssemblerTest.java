package io.openpixee.java;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codemodder.CodemodRegulator;
import io.codemodder.DefaultRuleSetting;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

final class VisitorAssemblerTest {

  @Test
  void it_filters_file_visitors_based_on_configuration() {
    VisitorAssembler assembler = VisitorAssembler.createDefault();
    CodemodRegulator everythingButDependencyInjection =
        CodemodRegulator.of(
            DefaultRuleSetting.ENABLED, List.of("pixee:java/mvn-dependency-injection"));
    File repositoryRoot = new File(".");
    CodemodRegulator everything = CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of());
    List<FileBasedVisitor> allVisitors =
        assembler.assembleFileVisitors(repositoryRoot, everything, Collections.emptyList());
    List<FileBasedVisitor> allButOne =
        assembler.assembleFileVisitors(
            repositoryRoot, everythingButDependencyInjection, Collections.emptyList());

    assertThat(allVisitors.size(), equalTo(1));
    assertThat(allButOne.size(), equalTo(0));
  }
}

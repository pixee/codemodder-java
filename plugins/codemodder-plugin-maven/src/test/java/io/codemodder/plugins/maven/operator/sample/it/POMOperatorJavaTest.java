package io.codemodder.plugins.maven.operator.sample.it;

import io.codemodder.plugins.maven.operator.Dependency;
import io.codemodder.plugins.maven.operator.POMOperator;
import io.codemodder.plugins.maven.operator.ProjectModel;
import io.codemodder.plugins.maven.operator.ProjectModelFactory;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URISyntaxException;

public class POMOperatorJavaTest {
  @Test
  public void testInterop() throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    ProjectModel projectModel = ProjectModelFactory.load(POMOperatorJavaTest.class.getResource("pom.xml"))
        .withDependency(new Dependency("org.dom4j", "dom4j", "0.0.0", null, "jar", null))
        .build();

    POMOperator.modify(projectModel);
  }
}

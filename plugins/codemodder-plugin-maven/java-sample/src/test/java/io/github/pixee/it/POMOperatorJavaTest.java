package io.github.pixee.it;

import io.github.pixee.maven.operator.POMOperator;
import io.github.pixee.maven.operator.ProjectModelFactory;
import io.github.pixee.maven.operator.ProjectModel;
import org.dom4j.DocumentException;
import org.junit.Test;

import io.github.pixee.maven.operator.Dependency;

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

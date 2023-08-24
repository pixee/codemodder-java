package io.github.pixee.it;

import org.junit.Test;

import io.github.pixee.maven.operator.ProjectModel;
import io.github.pixee.maven.operator.Dependency;
import io.github.pixee.maven.operator.POMOperator;
import io.github.pixee.maven.operator.ProjectModelFactory;

public class POMOperatorJavaTest {
  @Test
  public void testInterop() {
    ProjectModel projectModel = ProjectModelFactory.load(POMOperatorJavaTest.class.getResource("pom.xml"))
        .withDependency(new Dependency("org.dom4j", "dom4j", "0.0.0", null, "jar", null))
        .build();

    POMOperator.modify(projectModel);
  }
}

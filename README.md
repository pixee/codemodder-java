[![Actions Status](https://github.com/openpixee/pom-operator/workflows/Java%20CI/badge.svg)](https://github.com/openpixee/pom-operator/actions)
![Coverage](.github/badges/jacoco.svg)

# pom-operator

POM Operator is a library responsible for injecting dependencies into POM files programatically.

## Building

Use [Maven](https://maven.apache.org):

```
$ git clone git@github.com:openpixee/pom-operator.git && cd pom-operator
$ mvn clean install
```

## Using

There's a sample of usage from Java on the `java-sample` directory - of look under the `src/test` directory as well. TL;DR:

```java
import org.junit.Test;

import io.openpixee.maven.operator.ProjectModel;
import io.openpixee.maven.operator.Dependency;
import io.openpixee.maven.operator.POMOperator;
import io.openpixee.maven.operator.ProjectModelFactory;

public class POMOperatorJavaTest {
  @Test
  public void testInterop() {
    ProjectModel projectModel = ProjectModelFactory
        .load(POMOperatorJavaTest.class.getResource("pom.xml"))
        .withDependency(
            new Dependency("org.dom4j", "dom4j", "0.0.0", null, "jar")
        );

    POMOperator.modify(projectModel);
  }
}

```

## How it works?

It implements a Chain of Responsibility strategy - each `Command` class attempts a different way of fixing a POM, based around a Context (in this case, a `ProjectModel`)


# TODO:

- ~~better readme~~
- be able to guess existing indenting for existing documents
- investigate leverage whats on [versions-maven-plugin](https://github.com/mojohaus/versions-maven-plugin)

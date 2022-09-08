# pom-operator

POM Operator is a Library responsible for maintaining POMS

## Building it

Use [Maven](https://maven.apache.org):

```
$ git clone git@github.com:openpixee/pom-operator.git && cd pom-operator
$ mvn clean install
```

## How to Use It?

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
    ProjectModel projectModel = ProjectModelFactory.load(POMOperatorJavaTest.class.getResource("pom.xml"), new Dependency("org.dom4j", "dom4j", "0.0.0", null, "jar"));

    POMOperator.upgradePom(projectModel);
  }
}

```

## How it works?

It implements a Chain of Responsibility strategy - each `Command` class attempts a different way of fixing a POM, based around a Context (in this case, a `ProjectModel`)


# TODO:

- ~~better readme~~
- be able to guess existing indenting for existing documents

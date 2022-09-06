package io.openpixee.it;

import org.junit.Test;

import java.io.File;

import io.openpixee.maven.operator.Context;
import io.openpixee.maven.operator.Dependency;
import io.openpixee.maven.operator.POMOperator;

public class POMOperatorJavaTest {
  @Test
  public void testInterop() {
    Context ctx = Context.Companion.load(POMOperatorJavaTest.class.getResource("pom.xml"), new Dependency("org.dom4j", "dom4j", "0.0.0", null, "jar"));

    POMOperator.INSTANCE.upgradePom(ctx);
  }
}

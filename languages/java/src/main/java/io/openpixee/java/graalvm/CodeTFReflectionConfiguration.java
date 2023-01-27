package io.openpixee.java.graalvm;

import static io.openpixee.java.graalvm.JavaParserReflectionConfiguration.registerPackageForReflection;

import org.graalvm.nativeimage.hosted.Feature;

public class CodeTFReflectionConfiguration implements Feature {

  public void beforeAnalysis(BeforeAnalysisAccess access) {
    registerPackageForReflection(access, "io.github.pixee.codetf");
  }

}

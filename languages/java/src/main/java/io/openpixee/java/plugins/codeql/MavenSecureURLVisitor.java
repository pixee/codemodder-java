package io.openpixee.java.plugins.codeql;

import com.contrastsecurity.sarif.Result;
import io.openpixee.java.ChangedFile;
import io.openpixee.java.FileBasedVisitor;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.WeavingResult;
import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an important default weaver that will inject the dependencies that the weaves require.
 */
public final class MavenSecureURLVisitor implements FileBasedVisitor {
  private File repositoryRoot;
  private Set<Result> results;

  public MavenSecureURLVisitor(File repositoryRoot, Set<Result> results) {
    this.repositoryRoot = repositoryRoot;
    this.results = results;
  }

  @Override
  public String ruleId() {
    return secureURLRuleId;
  }

  @Override
  public WeavingResult visitRepositoryFile(
      final File repositoryRoot,
      final File file,
      final FileWeavingContext weavingContext,
      final Set<ChangedFile> changedJavaFiles) {
    System.out.println("MAVENSECUREURL: " + file);
    return WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());
  }

  static final String secureURLRuleId = "codeql:java/maven-non-https-url";
  private static final Logger LOG = LoggerFactory.getLogger(MavenSecureURLVisitor.class);
}

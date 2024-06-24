package io.codemodder.remediation.jndiinjection;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/**
 * Remediates JNDI injection vulnerabilities. It does this by weaving in a check to limit what JNDI
 * resources are available, and users can add more.
 *
 * <p>Inspiration for this came from logback:
 * https://github.com/qos-ch/logback/blob/979d76f3f2847f1c129bcc6295e69187d02e472c/logback-core/src/main/java/ch/qos/logback/core/util/JNDIUtil.java#L54
 */
public interface JNDIInjectionRemediator {

  /** Remediate all JNDI injection vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);

  /** The default JNDI injection remediation strategy. */
  JNDIInjectionRemediator DEFAULT = new DefaultJNDIInjectionRemediator();
}

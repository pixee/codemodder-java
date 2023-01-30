package io.openpixee.codetl.test.integration.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotates an integration test parameter of type {@link Path} that should be resolved into a
 * cloned copy of the given git repository.
 *
 * <p>Git repositories are expected to be read-only. When multiple tests require the same git
 * repository in the same state (e.g. same commit), then one repository will be shared for all such
 * tests, to avoid redundant repository cloning.
 */
@ExtendWith(CloneRepositoryExtension.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CloneRepository {

  /**
   * @return remote git repository URI
   */
  String repo();

  /**
   * @return branch to clone
   */
  String branch();
}

package io.codemodder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.MissingResourceException;

/** A utility class for accessing a codemod's resources in its "default location" the classpath. */
public final class CodemodResources {

  private CodemodResources() {}

  /**
   * Returns a class resource as a {@code String}.
   *
   * <p>The absolute name of the class resource is of the following form:
   *
   * <blockquote>
   *
   * {@code /modifiedPackageName/className/relativeName}
   *
   * </blockquote>
   *
   * Where the {@code modifiedPackageName} is the package name of this object with {@code '/'}
   * substituted for {@code '.'}.
   *
   * @param type The codemod type.
   * @param relativeName The relative name of the resource.
   * @return The resource as a {@code String}.
   * @throws MissingResourceException If the resource was not found.
   */
  public static String getClassResourceAsString(final Class<?> type, final String relativeName) {
    String resourceName = "/" + type.getName().replace('.', '/') + "/" + relativeName;
    try (InputStream stream = type.getResourceAsStream(resourceName)) {
      if (stream == null) {
        throw new MissingResourceException(resourceName, type.getName(), resourceName);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

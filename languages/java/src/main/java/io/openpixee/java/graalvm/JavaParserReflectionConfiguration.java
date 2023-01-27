package io.openpixee.java.graalvm;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

public class JavaParserReflectionConfiguration implements Feature {

  public void beforeAnalysis(BeforeAnalysisAccess access) {
    registerPackageForReflection(access, "com.github.javaparser.ast");
  }

  /**
   * Registers all the classes under the specified package for reflection.
   */
  public static void registerPackageForReflection(FeatureAccess access, String packageName) {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    try {
      String path = packageName.replace('.', '/');

      Enumeration<URL> resources = classLoader.getResources(path);
      while (resources.hasMoreElements()) {
        URL url = resources.nextElement();

        URLConnection connection = url.openConnection();
        if (connection instanceof JarURLConnection) {
          List<String> classes = findClassesInJar((JarURLConnection) connection, packageName);
          for (String className : classes) {
            registerClassHierarchyForReflection(access, className);
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to load classes under package name.", e);
    }
  }

  private static List<String> findClassesInJar(
      JarURLConnection urlConnection, String packageName) throws IOException {

    List<String> result = new ArrayList<>();

    final JarFile jarFile = urlConnection.getJarFile();
    final Enumeration<JarEntry> entries = jarFile.entries();

    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String entryName = entry.getName();

      if (entryName.endsWith(".class")) {
        String javaClassName =
            entryName
                .replace(".class", "")
                .replace('/', '.');

        if (javaClassName.startsWith(packageName)) {
          result.add(javaClassName);
        }
      }
    }

    return result;
  }

  /**
   * Registers the transitive class hierarchy of the provided {@code className} for reflection.
   *
   * <p>The transitive class hierarchy contains the class itself and its transitive set of
   * *non-private* nested subclasses.
   */
  public static void registerClassHierarchyForReflection(FeatureAccess access, String className) {
    Class<?> clazz = access.findClassByName(className);
    if (clazz != null) {
      registerClassForReflection(access, className);
      for (Class<?> nestedClass : clazz.getDeclaredClasses()) {
        if (!Modifier.isPrivate(nestedClass.getModifiers())) {
          registerClassHierarchyForReflection(access, nestedClass.getName());
        }
      }
    } else {
      logger.warning(
          "Failed to find " + className + " on the classpath for reflection.");
    }
  }

  /**
   * Registers an entire class for reflection use.
   */
  public static void registerClassForReflection(FeatureAccess access, String name) {
    Class<?> clazz = access.findClassByName(name);
    if (clazz != null) {
      RuntimeReflection.register(clazz);
      RuntimeReflection.register(clazz.getDeclaredConstructors());
      RuntimeReflection.register(clazz.getDeclaredFields());
      RuntimeReflection.register(clazz.getDeclaredMethods());
    } else {
      logger.warning(
          "Failed to find " + name + " on the classpath for reflection.");
    }
  }

  private static final Logger logger = Logger.getLogger(
      JavaParserReflectionConfiguration.class.getName());
}

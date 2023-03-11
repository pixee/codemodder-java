package io.codemodder;

/** A provider that offers developers a way to query and manage dependencies. */
public interface DependencyManagementProvider {

  boolean hasDependency(String groupId, String artifactId, String minimumVersion);

  void addDependency(String groupId, String artifactId, String minimumVersion);
}

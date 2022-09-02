
## Tasks

  * Sample POM files to try out (and use as tests)
  * Test Cases
  * Docopt-ify
  * Mac/Win/Linux utilities?)

## Goals

I want to insert a dependency into the pom.xml files in my repositories in my organization.

I want a Java library that helps me inject this dependency into a repository already-checked out on disk. There are multiple cases that we must account for.

1. They already have a "dependencies" section with the given dependency in it, so it doesn't have to be inserted.

2. One of their parent POMs already has the "dependency" and it is inherited in the child POM, so it doesn't have to be inserted.

3. The POM has the dependency, but the version they have is behind, so it needs to be replaced.

4. The POM doesn't have a "dependencies" section or inheritance so we need to inject that section as well as the "dependency" section.

5. The POM doesn't have the dependency or inherit it, so it needs to be inserted.

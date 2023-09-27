This change replaces the usage of some [Apache Commons IO](https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/FileUtils.html) file reading methods in favor of [Java's NIO API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html). 

Java NIO has native tools and a non-blocking strategy that make it faster for many use cases. Commons IO is often brought into a project for a handful of quality-of-life `File`-related APIs. Migrating to NIO could allow the project to remove this dependency, reducing the artifact size and shrinking the attack surface.

```diff
- import org.apache.commons.io.FileUtils; 
+ import java.nio.file.Files;

- String fileContents = FileUtils.readFileToString(file);
+ String fileContents = Files.readString(file.toPath());
```

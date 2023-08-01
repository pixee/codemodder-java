This change replaces the usage of [`java.io.File#createTempFile`](https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/io/File.html#createTempFile(java.lang.String,java.lang.String)) with [`java.nio.file.Files#createTempFile`](https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/nio/file/Files.html#createTempFile(java.lang.String,java.lang.String,java.nio.file.attribute.FileAttribute...)) which has more secure attributes.

The `java.io.File#createTempFile()` method creates a file that is world-readable and world-writeable, which is almost never necessary. Also, the file created is placed in a predictable directory (e.g., `/tmp`). Having predictable file names, locations, and will lead to many types of vulnerabilities. History has shown that this insecure pattern can lead to [information leakage](https://www.cvedetails.com/cve/CVE-2021-28168/), [privilege escalation](https://www.cvedetails.com/cve/CVE-2021-29428/) and even [code execution](https://www.openwall.com/lists/oss-security/2022/02/25/3).

Our changes look something like this:

```diff
-  File txtFile = File.createTempFile("acme", ".txt");
+  File txtFile = Files.createTempFile("acme", ".txt").toFile();
```

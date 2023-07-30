This change prevents a file descriptor leak and modernizes the file writing API pattern.

The way the code is written now, the [FileWriter](https://docs.oracle.com/javase/8/docs/api/java/io/FileWriter.html) never gets closed. Thus, it is up to the garbage collector's objection finalization process to close them at some point. This is not a good practice, and it can lead to a file descriptor leak. In hot code paths, it could cause exhaustion of all the available file descriptors for the system and lead to denial-of-service conditions.

Our changes look something like this:

```diff
-  BufferedWriter writer = new BufferedWriter(new FileWriter(f));
+  BufferedWriter writer = Files.newBufferedWriter(f.toPath());
```

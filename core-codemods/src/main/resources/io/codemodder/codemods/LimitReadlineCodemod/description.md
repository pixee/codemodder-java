This change hardens all [`BufferedReader#readLine()`](https://docs.oracle.com/javase/8/docs/api/java/io/BufferedReader.html#readLine--) operations against catastrophic conditions.

There is no way to call `readLine()` safely since it is, by its nature, a read that must be terminated by the stream provider. A stream influenced by an attacker could keep providing bytes until the JVM runs out of memory.

Fixing it is straightforward using an API which limits the amount of expected characters to some sane limit. This is what our changes look like:

```diff
+ import io.github.pixee.security.BoundedLineReader;
  ...
  BufferedReader reader = getReader();
- String line = reader.readLine(); // unlimited read, can lead to DoS
+ String line = BoundedLineReader.readLine(reader, 5_000_000); // limited to 5MB
```

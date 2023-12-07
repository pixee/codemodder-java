This change adds private constructors to utility classes. Utility classes are only meant to be accessed statically. Since they're not meant to be instantiated, we can use the Java's code visibility protections to hide the constructor and prevent unintended or malicious access.

Our changes look something like this:

```diff
   public class Utils {
+    private Utils() {}
     ...
```

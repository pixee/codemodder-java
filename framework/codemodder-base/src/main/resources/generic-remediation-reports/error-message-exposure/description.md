This change adds a timout to regex matching calls from the `java.util.regex` libraries.

Our changes look like this:

```java
+public <E> E executeWithTimeout(final Callable<E> action, final int timeout){
+    Future<E> maybeResult = Executors.newSingleThreadExecutor().submit(action);
+    try{
+        return maybeResult.get(timeout, TimeUnit.MILLISECONDS);
+    }catch(Exception e){
+        throw new RuntimeException("Failed to execute within time limit.");
+    }
+}
...
String input = "aaaaaaaaaaaaaaaaaaaaa";
Pattern pat = Pattern.compile("^(a+)+$");
var matcher = pat.matcher(input);
- matcher.matches();
+ executeWithTimeout(() -> matcher.matches(), 5000);
```

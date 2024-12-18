This change removes exceptions that Sonar believes are not possible to be thrown. Cleaning these improves readability and helps callers of the method understand how to effectively use the methods.

Our changes look something like this:

```diff
-  void it_throws_stuff() throws URISyntaxException {
+  void it_throws_stuff() {
     // not yet implemented
   }     
```

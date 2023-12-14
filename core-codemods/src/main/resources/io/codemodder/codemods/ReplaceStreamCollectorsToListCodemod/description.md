TODO

Our changes look something like this:

```diff
     List<CodemodChange> allWeaves =
-        linesAffected.stream().map(CodemodChange::from).collect(Collectors.toList());
+        linesAffected.stream().map(CodemodChange::from).toList();
```

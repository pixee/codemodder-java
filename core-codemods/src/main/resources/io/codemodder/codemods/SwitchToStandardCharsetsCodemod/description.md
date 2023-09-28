This change removes character set lookups with hardcoded strings like `"UTF-8"` in favor of referencing the `StandardCharsets` constants, which were [introduced in Java 7](https://docs.oracle.com/javase/7/docs/api/java/nio/charset/StandardCharsets.html).

This is faster, more predictable, and will remove the need for handling the `UnsupportedEncodingException`, which makes code easier to reason about. It will also remove IDE and compiler warnings.

Our changes look something like this:

```diff
  String s = getPropertyValue();
- byte[] b = s.getBytes("UTF-8");
+ byte[] b = s.getBytes(StandardCharsets.UTF_8);
```

Note: Further changes to exception handling may be needed.

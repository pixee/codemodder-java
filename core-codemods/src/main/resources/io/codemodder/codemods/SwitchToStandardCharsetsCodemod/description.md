This change removes character set lookups with hardcoded strings like `"UTF-8"` in favor of referencing the `StandardCharsets` constants.

This is faster, more predictable, and will remove the need for handling the `UnsupportedEncodingException`, which makes code easier to reason about. It will also remove compiler warnings.

Our changes look something like this:

```diff
  String s = getPropertyValue();
- byte[] b = s.getBytes("UTF-8");
+ byte[] b = s.getBytes(StandardCharsets.UTF_8);
```

Note: more changes related to exception handling may be needed.

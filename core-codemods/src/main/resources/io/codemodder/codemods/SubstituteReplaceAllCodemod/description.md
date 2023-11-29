This change substitutes the usage of `String#replaceAll()` to `String#replace()`.

Changes:

```diff
    String init = "my string\n";

-   String changed = init.replaceAll("\n", "<br>");
+   String changed = init.replace("\n", "<br>");
```

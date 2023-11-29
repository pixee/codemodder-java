This change replaces the usage of `String#replaceAll()` to `String#replace()` when the first argument is not a regular expression. It does exactly the same thing as `String#replaceAll()` without the performance drawback of the regex.

Changes:

```diff
    String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";

-   String changed = init.replaceAll("Bob is", "It's");
+   String changed = init.replaceAll("\\w*\\sis", "It's");
```

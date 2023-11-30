The code adjustment involves replacing the use of `String#replaceAll()` with `String#replace() for better efficiency. This update is driven by the need to enhance performance by avoiding the overhead incurred by the replaceAll() method, particularly when the first argument provided isn't a true regular expression.

Changes implemented:

```diff
    String init = "my string\n";

-   String changed = init.replaceAll("\n", "<br>");
+   String changed = init.replace("\n", "<br>");
```

By leveraging String#replace() instead of String#replaceAll(), the code maintains the same functionality while mitigating the performance cost associated with unnecessary regular expression compilation. This optimization adheres to best practices outlined in the Sonar rule, ensuring better code performance without compromising functionality.

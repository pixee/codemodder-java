This change moves the array designators next to the type instead of being next to the variable name to improve code readability.

Our changes look something like this:

```diff
-       byte buffer[];
+       byte [] buffer;
    }
```

This change uses the correct Integer parse method to make the code more efficient and the intent of the developer clearer

Changes:

```diff
    String myNum = "42.0";

-   int myInteger = Integer.valueOf(myNum);
+   int myInteger = Integer.parseInt(myNum);
```

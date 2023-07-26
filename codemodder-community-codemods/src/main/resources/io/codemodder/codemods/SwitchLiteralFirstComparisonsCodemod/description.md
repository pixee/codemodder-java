This changes literals

```diff
  try {
    httpResponse.write(output);
  } catch (Exception e) {
-   response.sendError(401, e.getMessage());
+   response.sendError(401);
  }
```

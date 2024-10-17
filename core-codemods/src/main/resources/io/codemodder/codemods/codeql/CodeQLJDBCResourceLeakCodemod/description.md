This change adds [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) to JDBC code to prevent database resources from being leaked, which could lead to denial-of-service conditions like connection pool or file handle exhaustion.

Our changes look something like this:

```diff
- Statement stmt = conn.createStatement();
- ResultSet rs = stmt.executeQuery(query);
- // do stuff with results
+ try (Statement stmt = conn.createStatement()) {
+   ResultSet rs = stmt.executeQuery(query);
+   // do stuff with results
+ }
```

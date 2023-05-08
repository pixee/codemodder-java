This change prevents stack trace information from reaching the HTTP response, which could leak code internals to an attacker and aid in further profiling and attacks.

Have you ever seen an error page and thought, "Wow, I certainly shouldn't be seeing all these code details?" That's this problem.

Switching to a safe signature that doesn't leak anything is easy and the changes look something like this:

```diff
try {
  httpResponse.write(output);
} catch (Exception e) {
-  response.sendError(401, e.getMessage());
+  response.sendError(401);
}
```

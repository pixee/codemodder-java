TODO

Our changes look something like this:

```diff
     catch (final UnsolvedSymbolException e) {
-      String errorMessage = "An unexpected exception happened";
       LOG.error("Problem resolving type of : {}", expr, e);
       return false;
     }
```

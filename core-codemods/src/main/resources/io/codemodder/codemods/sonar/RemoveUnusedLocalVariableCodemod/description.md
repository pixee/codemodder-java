This change removes unused variables. Unused variables make the code harder to read, which will lead to confusion and bugs. We only remove variables that have no state-changing effects.

Our changes look something like this:

```diff
     catch (final UnsolvedSymbolException e) {
-      String errorMessage = "An unexpected exception happened";
       LOG.error("Problem resolving type of : {}", expr, e);
       return false;
     }
```

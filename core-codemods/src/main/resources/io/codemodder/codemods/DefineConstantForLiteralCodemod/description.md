This change defines a constant for duplicated literal expression values, simplifying the refactoring process and mitigating the risk of overlooking any values during updates.

Our changes look something like this:

```diff

+    private static final String EXCEPTION_AT = "Exception at";

-       LOG.error("Exception at", ex)
+       builder.add(EXCEPTION_AT)  

-       LOG.error("Exception at", ex)
+       builder.add(EXCEPTION_AT)  

-       LOG.error("Exception at", ex)
+       builder.add(EXCEPTION_AT)  
```

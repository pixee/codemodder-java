This change moves the `default` case of `switch` statements to the end to match convention.

If code is hard to read, it is by definition hard to reason about during review and during coding in that area later. Not being able to quickly and effectively reason about code will lead to bugs, including security vulnerabilities. 

The `default` case is usually last. Being further up may cause confusion about how the code will flow as is shown in the example below, which will unfortunately :

```java
  switch (accessLevel) {
   default:
     access = false;
   case GRANTED:
     access = true;
     break;
   case REJECTED:
     access = false;
     break;
  }
```
In the case above, the 

Our changes look something like this:

```diff
  switch (accessLevel) {
-    default:
-      access = false;
     case GRANTED:
       access = true;
       break;
     case REJECTED:
       access = false;
       break;
+    default:
+      access = false;
  }
```

This change adds missing synchronized keyword to methods that override a synchronized method in the parent class.
Our changes look something like this:

```diff
  interface AcmeParent {
     synchronized void doThing();
  } 

  class AcmeChild implements AcmeParent {

    @Override
-    void doThing() {
+    synchronized void doThing() {
      thing();
    }
    
  }
```

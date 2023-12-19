This change removes unused `private` methods. Dead code can cause confusion and increase the mental load of maintainers. It can increase your maintenance burden as you have to keep that unused code compiling when you make sweeping changes to the APIs used within the method.

Our changes look something like this:

```diff
-   private String getUuid(){
-       return uuid;
-   }
```

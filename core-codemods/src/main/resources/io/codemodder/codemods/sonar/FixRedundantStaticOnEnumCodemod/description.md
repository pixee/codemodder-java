This change removes redundant (and possibly misleading) `static` keywords on `enum` types defined within classes. All `enum` types that are nested within another type are automatically `static`, and so listing the flag this clutters the code, and may cause confusion when reasoning about it.

Our changes look something like this:

```diff
  @RestController
  final class CheckStatusController {

-   static enum ResponseType {  
+   enum ResponseType {
      SUCCESS,
      FAILURE,
      ERROR
    }
```

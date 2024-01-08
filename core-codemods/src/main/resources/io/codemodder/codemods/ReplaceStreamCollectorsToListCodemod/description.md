This change modernizes a stream's `List` creation to be driven from the simple, and more readable [`Stream#toList()`](https://docs.oracle.com/javase/16/docs/api/java.base/java/util/stream/Collectors.html#toList()) method.

Our changes look something like this:

```diff
- List<Integer> numbers = someStream.collect(Collectors.toList());
+ List<Integer> numbers = someStream.toList();
```

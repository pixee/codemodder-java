Type inference is a feature introduced in Java 7 simplifying the building of generic classes.

When using type parameters, the compiler infers the type arguments based on context, rather than explicitly specifying them.

This change makes the code easier to review and removes IDE warnings that could harm readability.
```diff
- List<String> myList = new ArrayList<String>();

+ List<String> myList = new ArrayList<>();
```

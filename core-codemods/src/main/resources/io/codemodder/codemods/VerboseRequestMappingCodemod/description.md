This change simplifies spring framework annotations by making use of shortened annotations when applicable.

Version 4.3 introduced method-level variants for `@RequestMapping`.
- `@GetMapping`
- `@PutMapping`
- `@PostMapping`
- `@DeleteMapping`
- `@PatchMapping`

The [annotations](https://dzone.com/articles/using-the-spring-requestmapping-annotation) better express the semantics of the annotated methods. They act as wrapper to `@RequestMapping` and have become the standard ways of defining the endpoints.

```diff
- @RequestMapping(value = "/example", method = RequestMethod.GET)
  ...
+ @GetMapping(value = "/example")
```


This change simplifies Spring Framework annotations by making use of shortened annotations when applicable.
Code that is easy to read is easy to review, reason about, and detect bugs in.

Making use of shortcut annotations accomplishes this by removing *wordy for no reason* elements.  


Version 4.3 of Spring Framework introduced method-level variants for `@RequestMapping`.
- `@GetMapping`
- `@PutMapping`
- `@PostMapping`
- `@DeleteMapping`
- `@PatchMapping`

```diff
- @RequestMapping(value = "/example", method = RequestMethod.GET)
  ...
+ @GetMapping(value = "/example")
```


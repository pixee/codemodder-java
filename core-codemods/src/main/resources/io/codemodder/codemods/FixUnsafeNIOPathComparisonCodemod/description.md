This change makes path comparisons more reliable, more secure and may prevent a path traversal weakness.

The current code attempts to make sure that a file is within a directory by comparing strings. This is not safe to rely on because the parent directory may not always be provided in a `File` with the trailing slash. 

For instance, let's imagine we have a directory `/tmp/secure/` where we want to store files. Let's now imagine we want to check if `/tmp/foo/secure-bad.txt` is in this directory. 

It feels intuitively correct to try to compare the two paths using a `startsWith()` using the `File#getCanonicalPath()` from both items. However `getCanonicalPath()` will return a directory path _without_ the trailing slash, so the comparison will ask, "Does `/tmp/secure-bad.txt` start with `/tmp/secure`?". The answer would be yes, but this is not the safe and expected answer for the given paths. 

Our change converts the `File` to a `Path` and uses the `Path#startsWith()` API, which natively understands directories (and isn't comparing strings), and will return the intended answer. Here's an example of the change:

```diff
- if(child.getCanonicalPath().startsWith(parent.getCanonicalPath())) {
+ if(child.getCanonicalFile().toPath().startsWith(parent.getCanonicalFile().toPath())) {
    // after the change, the file is reliably in the expected directory
  }
```

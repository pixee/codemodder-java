This change attempts to remediate path traversal (also called directory traversal, local file include, etc.) vulnerabilities.

Our changes may introduce sanitization up front, if the input looks like it's a file name (like from a multipart HTTP request), or validation if it appears to be a piece of path.


```diff
+ import io.github.pixee.security.Filenames;

  ...
  
- String fileName = request.getFileName();
+ String fileName = Filenames.toSimpleFileName(request.getFileName());
```

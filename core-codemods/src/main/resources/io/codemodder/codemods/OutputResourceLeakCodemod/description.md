This change adds [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) to code to prevent resources from being leaked, which could lead to denial-of-service conditions like connection pool or file handle exhaustion.

Our changes look something like this:

```diff
- BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\test.txt"));
- bw.write("Hello world!");
+ try(FileWriter input = new FileWriter("C:\\test.txt")); BufferedWriter bw = new BufferedWriter(input)){ 
+   bw.write("Hello world!");
+ }
```

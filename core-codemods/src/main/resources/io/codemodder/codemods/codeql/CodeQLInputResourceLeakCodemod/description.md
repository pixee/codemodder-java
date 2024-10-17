This change adds [try-with-resources](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html) to code to prevent resources from being leaked, which could lead to denial-of-service conditions like connection pool or file handle exhaustion.

Our changes look something like this:

```diff
- BufferedReader br = new BufferedReader(new FileReader("C:\\test.txt"));
- System.out.println(br.readLine());
+ try(FileReader input = new FileReader("C:\\test.txt"); BufferedReader br = new BufferedReader(input)){
+   System.out.println(br.readLine());
+ }
```

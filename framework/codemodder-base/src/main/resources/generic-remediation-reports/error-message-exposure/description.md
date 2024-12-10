This change removes exposure through sending/printing of error and exception data.

Our changes look like this:

```java
 void function(HttpServletResponse response) {
    PrintWriter pw = reponse.getWriter();
    try{
        ...
    } catch (Exception e) {
-        pw.println(e.getMessage());
    }
 }
```

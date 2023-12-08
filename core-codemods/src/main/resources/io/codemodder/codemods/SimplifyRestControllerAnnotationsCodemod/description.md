This change makes it harder for developers to make a mistake when writing REST controllers in Spring. By marking the top level type with `@RestController`, it is now assumed that all the methods within it will return a Java object representing the response body. Thus, there is no need to specify, for each method, the `@ResponseBody` annotation.

Our changes look something like this:

```diff
-   import org.springframework.stereotype.Controller;
-   import org.springframework.web.bind.annotation.ResponseBody;
+   import org.springframework.web.bind.annotation.RestController;
-   @Controller
+   @RestController
    public class AccountController {
      ...
-     @ResponseBody
      public AccountDetails viewAccount() {
        ...
```

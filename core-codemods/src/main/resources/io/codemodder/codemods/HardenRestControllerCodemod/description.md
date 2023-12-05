// TODO

Our changes look something like this:

```diff
    import lombok.RequiredArgsConstructor;
    import org.owasp.webgoat.container.i18n.Messages;
    import org.owasp.webgoat.container.session.WebSession;
-   import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.RequestMapping;
-   import org.springframework.web.bind.annotation.ResponseBody;
+   import org.springframework.web.bind.annotation.RestController;
-   @Controller
+   @RestController
    @RequiredArgsConstructor
    public class SessionService {
      private final Messages messages;
      @RequestMapping(path = "/service/enable-security.mvc", produces = "application/json")
-     @ResponseBody
      public String applySecurity() {
        webSession.toggleSecurity();
        restartLessonService.restartLesson();
```

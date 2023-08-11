This change hardens all [`ServletRequest#getRequestDispatcher(String)`](https://docs.oracle.com/javaee/7/api/javax/servlet/ServletRequest.html#getRequestDispatcher-java.lang.String-) calls against attack.

There is a built-in HTTP method for sending clients to another resource: the [client-side redirect](https://developer.mozilla.org/en-US/docs/Web/HTTP/Redirections). However, the `getRequestDispatcher(String)` method is unique in that performs a forward which occurs totally within the _server-side_.

There is some security that usually comes within redirecting users back through the "front door". For instance, attackers could never directly request sensitive resources like `/WEB-INF/web.xml`. However, this isn't true for request dispatcher forwarding. Therefore, we must take special care that the path being forwarded isn't towards any known sensitive data.

Our change introduces an API that offers some validation against forwards that target sensitive data or attempt to access application code.

```diff
+ import static io.github.pixee.security.jakarta.PathValidator.validateDispatcherPath;
  ...
+ validateDispatcherPath(path);
  request.getRequestDispatcher(path).forward(request, response);
```

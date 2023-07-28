This change enforces a maximum cookie timeout to a value more in line with common security guidance. 

There are a few different types of session timeouts -- this change is focused on [absolute timeouts](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html#:~:text=an%20absolute%20timeout) -- how long an active user or client who is actively using the application can remain logged in. Without an absolute timeout, an attacker who has compromised a session token can remain logged in indefinitely.

Some industries have specific requirements for absolute timeouts, but in general, 8 hours is a popular default because it allows users to stay logged in for a work day. Absolute timeouts are also useful for applications that offer APIs as they limit the exposure of a stolen API token.

Our changes are usually introducing a new value to the `application.properties` file, like this:

```diff
  server.servlet.session.timeout=10m
+ spring.servlet.session.cookie.max-age=8h
```

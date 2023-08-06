This change adds logging of failed login attempts, which is useful for detecting
and preventing brute force attacks and other malicious activities.

```diff
  else {
+   logger.warn("Failed login attempt for user: {}", username);
    response.sendRedirect("LoginPage?error=Invalid User or Password");
  }
```

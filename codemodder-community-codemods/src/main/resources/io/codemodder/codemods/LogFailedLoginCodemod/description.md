This change adds logging of failed login attempts, which is useful for detecting
and preventing brute force attacks and other malicious activities.

```diff
  else {
+   System.out.println("Failed login attempt for user: " + user);
    response.sendRedirect("LoginPage?error=Invalid User or Password");
  }
```

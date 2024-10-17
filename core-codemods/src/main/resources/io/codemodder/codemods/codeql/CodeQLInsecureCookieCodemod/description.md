This change marks new cookies sent in the HTTP with the ["secure" flag](https://owasp.org/www-community/controls/SecureCookieAttribute). This flag, despite its ambitious name, only provides one type of protection: confidentiality. Cookies with this flag are guaranteed by the browser never to be sent over a cleartext channel ("http://") and only sent over secure channels ("https://").

Our change introduces this flag with a simple 1-line statement:

```diff
  Cookie cookie = new Cookie("my_cookie", userCookieValue);
+ cookie.setSecure(true);
  response.addCookie(cookie);
```

Note: this code change **may cause issues** with the application if any of the places this code runs (in CI, pre-production or in production) are running in non-HTTPS protocol.

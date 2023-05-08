This change encodes certain JSP scriptlets to fix what appear to be trivially exploitable [Reflected Cross-Site Scripting (XSS)](https://portswigger.net/web-security/cross-site-scripting) vulnerabilities in your JSP files. XSS is a vulnerability that is tricky to understand initially, but really easy to exploit.

Consider the following example code:

```java
Welcome to our site <%= request.getParameter("name") %>
```

An attacker could construct a link with an HTTP parameter `name` containing malicious JavaScript and send it to the victims, and if they click it, cause it to execute in the victims' browsers in the domain context. This could allow attackers to exfiltrate session cookies and spoof their identity, perform actions on victim's behalf, and more generally "do anything" as that user.

Our changes introduce an HTML-encoding mechanism that look something like this:

```diff
-Welcome to our site <%= request.getParameter("name") %>
+Welcome to our site <%= io.github.pixee.security.HtmlEncoder.encode(request.getParameter("name")) %>
```

This change neutralizes the control characters that attackers would use to execute code. Depending on the context in which the scriptlet is rendered (e.g., inside HTML tags, HTML attributes, in JavaScript, quoted contexts, etc.), you may need to use another encoder. Check out the [OWASP XSS Prevention CheatSheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html) to learn more about these cases and other controls you may need.

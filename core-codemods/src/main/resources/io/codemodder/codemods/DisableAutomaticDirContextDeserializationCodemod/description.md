This change patches the LDAP interaction code to harden against a remote code execution vulnerability.

Using Java's deserialization APIs on untrusted data [is dangerous](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html) because side effects from a type's reconstitution logic can be chained together to execute arbitrary code. This very serious and very common [bug class](https://github.com/GrrrDog/Java-Deserialization-Cheat-Sheet) has resulted in some high profile vulnerabilities, including the [log4shell vulnerability](https://en.wikipedia.org/wiki/Log4Shell) that rocked the development and security world (and is [_still_ present in organizations](https://www.wired.com/story/log4j-log4shell-one-year-later/), by the way.)

Now, back to the change. The `DirContext#search(SearchControls)` API is used to send LDAP queries. If the `SearchControls` has the `retobj` set to `true`, the API will try to deserialize a piece of the response from the LDAP server with Java's deserialization API. This means that if the LDAP server could influenced to return malicious data (or is outright controlled by an attacker) then they could [execute arbitrary on the API client's JVM](https://www.blackhat.com/docs/us-16/materials/us-16-Munoz-A-Journey-From-JNDI-LDAP-Manipulation-To-RCE.pdf).

Our changes look like this:

```diff
  DirContext ctx = new InitialDirContext();
- var results = ctx.search("query", "filter", new SearchControls(0, 0, 0, null, true, false));
+ var results = ctx.search("query", "filter", new SearchControls(0, 0, 0, null, false, false));
```

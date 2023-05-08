This change sandboxes the creation of [`java.net.URL`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URL.html) objects so they will be more resistant to Server-Side Request Forgery (SSRF) attacks.

Most of the time when you create a URL, you're intending to reference an HTTP endpoint, like an internal microservice. However, URLs can point to local file system files, a Gopher stream in your local network, a JAR file on a remote Internet site, and all kinds of other unexpected and undesirable stuff. When the URL values are influenced by attackers, they can trick your application into fetching internal resources, running malicious code, or otherwise harming the system. Consider the following code:

```java
String url = userInput.getServiceAddress();
return IOUtils.toString(new URL(url).openConnection());
```

In this case, an attacker could supply a value like `jar:file:/path/to/appserver/lib.jar` and attempt to read the contents of your application's code.

Our changes introduce sandboxing around URL creation that force the developers to specify some boundaries on the types of URLs they expect to create:

```diff
+import io.github.pixee.security.Urls;
+import io.github.pixee.security.HostValidator;
...
String url = userInput.getServiceAddress();
-URL u = new URL(url);
+URL u = Urls.create(url, Urls.HTTP_PROTOCOLS, HostValidator.DENY_COMMON_INFRASTRUCTURE_TARGETS);
InputStream is = u.openConnection();
```

This change alone reduces attack surface significantly, but can be enhanced to create even more security by specifying some controls around the hosts we expect to connect with:

```diff
+import io.github.pixee.security.Urls;
+import io.github.pixee.security.HostValidator;
...
HostValidator allowsOnlyGoodDotCom = HostValidator.fromAllowedHostPattern(Pattern.compile("good\\.com"));
URL u = Urls.create(url, Urls.HTTP_PROTOCOLS, allowsOnlyGoodDotCom);
```

Note: Beware temptation to write some validation on your own. Parsing URLs is difficult and differences between parsers in validation and execution will certainly lead to exploits as attackers [have repeatedly proven](https://www.blackhat.com/docs/us-17/thursday/us-17-Tsai-A-New-Era-Of-SSRF-Exploiting-URL-Parser-In-Trending-Programming-Languages.pdf).

This change updates all instances of [XMLInputFactory](https://docs.oracle.com/javase/8/docs/api/javax/xml/stream/XMLInputFactory.html) to prevent them from resolving external entities, which can protect you from arbitrary code execution, sensitive data exfiltration, and probably a bunch more evil things attackers are still discovering.

Without this protection, attackers can cause your `XMLInputFactory` parser to retrieve sensitive information with attacks like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
<book>
    <title>&xxe;</title>
</book>
```

Yes, it's pretty insane that this is the default behavior. Our change hardens the factories created with the necessary security features to prevent your parser from resolving external entities.

```diff
+ import io.github.pixee.security.XMLInputFactorySecurity;
  ...
- XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
+ XMLInputFactory xmlInputFactory = XMLInputFactorySecurity.hardenFactory(XMLInputFactory.newFactory());
```

You could take our protections one step further by changing our supplied code to prevent the user from supplying a `DOCTYPE`, which is more aggressive and more secure, but also more likely to affect existing code behavior:
```diff
+ import io.github.pixee.security.XMLInputFactorySecurity;
+ import io.github.pixee.security.XMLRestrictions;
  ...
  XMLInputFactory xmlInputFactory = XMLInputFactorySecurity.hardenFactory(XMLInputFactory.newFactory(), XMLRestrictions.DISALLOW_DOCTYPE);
```

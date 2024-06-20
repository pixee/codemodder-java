This change prevents XML parsing APIs from resolving external entities, which can protect you from arbitrary code execution, sensitive data exfiltration, and probably a bunch more evil things attackers are still discovering.

Without this protection, attackers can cause your parser to retrieve sensitive information with attacks like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
<book>
    <title>&xxe;</title>
</book>
```

Yes, it's pretty insane that this is the default behavior. Our change hardens the factories created with the necessary security features to prevent your parser from resolving external entities.

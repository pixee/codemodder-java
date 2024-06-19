This change updates [XMLReader](https://docs.oracle.com/en/java/javase/17/docs/api/java.xml/org/xml/sax/XMLReader.html) to prevent resolution of external entities, which can protect you from arbitrary code execution, sensitive data exfiltration, and probably a bunch more evil things attackers are still discovering.

Without this protection, attackers can cause your `XMLReader` parser to retrieve sensitive information with attacks like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
<book>
    <title>&xxe;</title>
</book>
```

Yes, it's pretty insane that this is the default behavior. Our change hardens the reader with the necessary security features to prevent your parser from resolving external entities.

```diff
  XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
  StringReader sr = new StringReader(xml);
+ reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
+ reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
  reader.parse(new InputSource(sr));
```

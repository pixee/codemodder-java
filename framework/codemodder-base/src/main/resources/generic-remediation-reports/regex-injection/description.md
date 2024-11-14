This change fixes [Regex Injection](https://wiki.sei.cmu.edu/confluence/display/java/IDS08-J.+Sanitize+untrusted+data+included+in+a+regular+expression) vulnerabilities by escaping the user input before using it in a regular expression. This is important because untrusted input can contain special characters that can change the behavior of the regular expression, leading to security vulnerabilities like denial of service, or change the application behavior to match in unexpected situations, possibly causing logical vulnerabilities.

Our changes look like this:

```java
  import java.util.regex.Pattern;

  // run the regex
- Pattern p = Pattern.compile(userInput);
+ Pattern p = Pattern.compile(Pattern.quote(userInput));
  Matcher m = p.matcher(input);
  if (m.find()) {
    // do something
  } 
```

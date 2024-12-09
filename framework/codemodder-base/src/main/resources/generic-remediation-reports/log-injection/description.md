This change ensures that log messages can't contain newline characters, leaving you vulnerable to Log Forging / Log Injection.

If malicious users can get newline characters into a log message, they can inject and forge new log entries that look like they came from the server, and trick log analysis tools, administrators, and more. This leads to vulnerabilities like Log Injection, Log Forging, and more attacks from there.

Our change simply strips out newline characters from log messages, ensuring that they can't be used to forge new log entries.
```diff
+ import io.github.pixee.security.Newlines;
  ...
  String orderId = getUserOrderId();
- log.info("User order ID: " + orderId);
+ log.info("User order ID: " + Newlines.stripNewlines(orderId));
```

This change remediates the Reflection Injection findings from Semgrep.

Our changes either suppress false positives, add security controls, or call out locations that may need further review.

Here's what it looks like when we add a compensating security control in cases that are suspicious:
```diff
+ import io.github.pixee.Reflections.loadAndVerify;
  ...
- Class<?> c = Class.forName(httpRequest.parameter("driver"));
+ Class<?> c = loadAndVerify(httpRequest.parameter("driver")); // prevent loading dangerous types
```

Here's what it looks like when we suppress a false positive:
```diff
+ // ok: java.lang.security.audit.unsafe-reflection.unsafe-reflection
  Class<?> c = Class.forName(System.get("DRIVER"));
```

If there are minor refactorings we can do to "make the tool happy", we may do that as well to prevent future tools from reporting it in order to prevent cluttering the source code with static analysis configuration.
```diff

  private static final String DRIVER = "com.mysql.jdbc.Driver";
  public String getDriver() {
     return DRIVER;
  }

- Class<?> c = Class.forName(getDriver()); // the tool can't tell that this is fine 
+ Class<?> c = Class.forName(DRIVER); // the tool now realizes that this is a constant
```

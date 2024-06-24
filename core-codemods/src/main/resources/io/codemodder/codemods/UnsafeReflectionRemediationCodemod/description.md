This change fixes Sonar's [Classes should not be loaded dynamically](https://rules.sonarsource.com/java/RSPEC-2658/) issue by replacing the unsafe class load with a hardened alternative.

The hardened alternative blocks the loading of classes that are well-known to be used by attackers to exploit the application.

Our changes look something like this:

```diff
- Class<MyStrategy> clazz = Class.forName(untrustedInput);
+ Class<MyStrategy> clazz = Reflection.loadAndVerify(untrustedInput);
```

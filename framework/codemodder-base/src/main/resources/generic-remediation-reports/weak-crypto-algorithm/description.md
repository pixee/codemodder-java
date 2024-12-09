This change ensures that weak cryptographic algorithms are not used in the application. Weak cryptographic algorithms are algorithms that are known to be vulnerable to attacks, and should not be used in secure applications. They can be exploited by attackers to decrypt sensitive data, impersonate users, and perform other malicious activities.

In the past, these events were considered more hypothetical, but with modern access to scalable compute, attacks have become more and more practical. For example, [multiple real-world attacks](https://en.wikipedia.org/wiki/SHA-1#Attacks) have been demonstrated against SHA-1.

Our changes look something like this:

```diff
- MessageDigest md = MessageDigest.getInstance("MD5");
+ MessageDigest md = MessageDigest.getInstance("SHA-256");
```

This change fixes [Reflection Injection](https://owasp.org/www-community/vulnerabilities/Unsafe_use_of_Reflection) vulnerabilities by limiting what types can be loaded. 

Without a protection like this, attackers can cause arbitrary classes to be loaded, methods to be executed, etc., in your application, which could lead to remote code execution, denial of service, or other unwanted behaviors.

Depending on how the reflection APIs are used and what types are available on the classpath, this vulnerability can be quite dangerous, and in fact GitHub itself [had a High-severity vulnerability of this type recently](https://github.com/advisories/GHSA-g39r-hh73-78xj) .

This change can be improved further by adding more restrictions like this available in the [`Reflection` API](https://github.com/pixee/java-security-toolkit/blob/main/src/main/java/io/github/pixee/security/Reflection.java).

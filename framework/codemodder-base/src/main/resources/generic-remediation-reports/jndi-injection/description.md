This change fixes JNDI Injection vulnerabilities by limiting what JNDI resources can be accessed. 

JNDI Injection is a vulnerability that occurs when an attacker can inject arbitrary URLs into a JNDI lookup call. This can lead to [remote code execution](https://www.blackhat.com/docs/us-16/materials/us-16-Munoz-A-Journey-From-JNDI-LDAP-Manipulation-To-RCE.pdf), denial of service, or other security vulnerabilities. 

Although it's not as well known as other vulnerability classes, it can be devastating. In fact, the famous [log4shell exploit](https://en.wikipedia.org/wiki/Log4Shell) which took the world by storm was JNDI Injection vulnerability.

This change adds a validation step before [Context#lookup()](https://docs.oracle.com/javase%2F8%2Fdocs%2Fapi%2F%2F/javax/naming/Context.html#lookup-java.lang.String-) calls, making sure the lookup target is a named resource previously defined, and not a URL which could point to arbitrary resources, like an evil RMI or LDAP server hosted by an attacker.

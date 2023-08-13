# `pixee:java/log-failed-login` examples

## Safe

* [`AuthProvider.java.before`](safe/AuthProvider.java.before) - Code with an `authenticate` method
  that does not validate user login credentials ([source](https://github.com/joscha/play-authenticate/blob/46ecb57dfb6e42f609d3cd79d6c507e90f247372/code/app/com/feth/play/module/pa/providers/AuthProvider.java)).
* [`JaasAuthenticationBroker.java.before`](safe/JaasAuthenticationBroker.java.before) -
  [Java Authentication and Authorization Service](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jaas/JAASRefGuide.html)
  (JAAS) example from ActiveMQ ([source](https://github.com/apache/activemq/blob/673f4b33e8eea5899805cf21a7548ac4c9f1b6a8/activemq-broker/src/main/java/org/apache/activemq/security/JaasAuthenticationBroker.java)).
  Throws a `SecurityException` on failed login attempts.
* [`LoginServlet.java.before`](safe/LoginServlet.java.before) - Servlet that logs a warning using
  SLF4J on failed login attempts.
* [`Main.java.before`](safe/Main.java.before) - Program that logs an error using Log4j 2 on failed
  login attempts.
* [`MainPrint.java.before`](safe/MainPrint.java.before) - Program that prints a message to the
  console on failed login attempts.
* [`Queue.java.before`](safe/Queue.java.before) - Large file with an `authenticate` method, which
  should get ignored because it will require too many tokens ([source](https://github.com/damianszczepanik/jenkins/blob/213034be01616bb5f30f2afadfd177f82daa2bc4/core/src/main/java/hudson/model/Queue.java)).
* [`SaltedHashLoginModule.java.before`](safe/SaltedHashLoginModule.java.before) - JAAS `LoginModule`
  that logs a message using SLF4J on failed login attempts ([source](https://github.com/georgleber/saltedhash-jaas-module/blob/d9573906d1f7ccfbe090a21324f0f9a35387492f/src/main/java/de/meetwithfriends/security/jaas/SaltedHashLoginModule.java)).

## Vulnerable

* [`LoginServlet.java.before`](vulnerable/LoginServlet.java.before) - Servlet that returns a message
  to the client on failed login attempts but does not log anything. This file intentionally uses DOS
  line separators (i.e. CR+LF).
* [`LoginValidate.java.before`](vulnerable/LoginValidate.java.before) - Servlet that redirects a
  client on failed login attempts but does not log anything ([source](https://github.com/vikkyp20/LoginModule/blob/d6ca86391b1e5961fb213366a1eb1b1bbb35b751/src/java/login/LoginValidate.java)).
* [`MainFrame.java.before`](vulnerable/MainFrame.java.before) - `JFrame` login form that imports
  `java.util.logging` (JUL) but does not log anything on failed login attempts ([source](https://itsourcecode.com/free-projects/java-projects/login-code-in-java-with-source-code/)).
  This file is intentionally poorly formatted and includes leading and trailing whitespace.

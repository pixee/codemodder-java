rule pixee:java/sandbox-url-creation

match
  ConstructorCall $c {
    type = java.net.URL
  }

require dependency io.openpixee:java-security-toolkit:1.0.0
require import io.openpixee.security.Urls

replace $c with
  StaticMethodCall {
    type = io.openpixee.security.Urls
    name = create
    args =  [Urls.DENY_COMMON_INFRASTRUCTURE_TARGETS, Urls.HTTP_PROTOCOLS] + $c.args
  }

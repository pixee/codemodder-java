rule pixee:java/sandbox-url-creation

match
  ConstructorCall $c {
    type = java.net.URL
  }

require dependency io.pixee:io.pixee.security:1.0
require import io.pixee.security.Urls

replace $c with
  StaticMethodCall {
    type = io.pixee.security.Urls
    name = create
    args =  [Urls.DENY_COMMON_INFRASTRUCTURE_TARGETS, Urls.HTTP_PROTOCOLS] + $c.args
  }

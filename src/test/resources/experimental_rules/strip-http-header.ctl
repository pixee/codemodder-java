rule pixee:java/strip-http-header-newlines

match
  InstanceMethodCall $c {
    type = javax.servlet.http.HttpServletResponse
    name = setHeader
    args = [*, *:StringLiteral]
  }

where
  !hasUpstream($c.args[1],
    StaticMethodCall {
      type = io.pixee.security.Newlines
      name = stripAll
      args = [$c.args[1]]
    })

require dependency io.pixee:io.pixee.security:1.0
require import org.pixee.security.Newlines

insert into data flow after $c
  StaticMethodCall {
    type = io.pixee.security.Newlines
    name = stripAll
    args = [$c]
  }


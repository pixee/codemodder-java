rule pixee:java/validate-jakarta-forward-path

match
  InstanceMethodCall $c {
    type in [javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest]
    name = getRequestDispatcher
    args = [!StringLiteral]
  }

where
  !hasDownstream($c.args[0],
    StaticMethodCall {
      name =~ validate
      args = [$c.args[0]]
    }
  )

require dependency io.openpixee:java-security-toolkit:1.0.0
require import org.openpixee.security.Jakarta

insert into data flow after $c.args[0]
  StaticMethodCall {
    type = io.openpixee.security.Jakarta
    name = validateForwardPath
    args = [$c.args[0]]
  }


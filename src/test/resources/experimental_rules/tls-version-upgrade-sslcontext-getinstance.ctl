match
  StaticMethodCall $c {
    type = javax.net.ssl.SSLContext
    name = getInstance
    args = [!StringLiteral]
  }


replace $c.args[0] with
  # how do we express this? bad strawman here
  LiteralExpression {
    type = java.lang.String[]
    value = ["TLSv1.2"]
  }

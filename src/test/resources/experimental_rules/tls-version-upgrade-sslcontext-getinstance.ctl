match
  StaticMethodCall $c {
    type = javax.net.ssl.SSLContext
    name = getInstance
    args = [!StringLiteral]
  }

replace $c.args[0] with
  StaticMethodCall {
    type = Security
    name = sanitize
    args = ["TLSv1.2"]
  }

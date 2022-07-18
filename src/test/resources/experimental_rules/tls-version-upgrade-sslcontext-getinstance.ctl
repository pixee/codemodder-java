GIVEN METHOD_CALL $getInstance WHERE
  name = getInstance
  type = javax.net.ssl.SSLContext
  arguments.size = 1
  traceConstantValue(arguments[0]) != "TLSv1.2"

TRANSFORM
  $getInstance.arguments[0] = "TLSv1.2"

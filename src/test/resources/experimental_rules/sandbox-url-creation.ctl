GIVEN METHOD_CALL $createUrl WHERE
  name = <init>
  type = java.net.URL
  foreach argument in arguments {
     isConstant(argument)
  }

TRANSFORM
  $createUrl.arguments.insert(Urls.DENY_COMMON_INFRASTRUCTURE_TARGETS)
  $createUrl.arguments.insert(Urls.HTTP_PROTOCOLS)
  METHOD_CALL safeCreateUrl := io.pixee.security.Urls.create($createUrl.arguments)
  RETURN safeCreateUrl

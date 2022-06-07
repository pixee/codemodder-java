GIVEN METHOD_CALL $createUrl WHERE
  name = <init>
  type = java.net.URL
  foreach argument in arguments {
     isConstant(argument)
  }

TRANSFORM
  $createUrl.arguments.insert(SafeURL.DENY_COMMON_INFRASTRUCTURE_TARGETS)
  $createUrl.arguments.insert(SafeURL.HTTP_PROTOCOLS)
  METHOD_CALL safeCreateUrl := io.pixee.SSRF.createSafeUrl($createUrl.arguments)
  RETURN safeCreateUrl

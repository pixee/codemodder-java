GIVEN METHOD_CALL $setHeaderCall WHERE
  name = setHeader
  arguments.size = 2
  arguments[1].nodeType != StringLiteral
  type = javax.servlet.http.HttpServletResponse
  arguments[1].code !=~ validate
  arguments[1].code !=~ sanitize
  arguments[1].code !=~ strip

TRANSFORM
  METHOD_CALL sanitized := org.pixee.security.HttpHeader.stripNewLines($ARGUMENTS[1])
  RETURN $setHeaderCall($ARGUMENTS[0], $sanitized)

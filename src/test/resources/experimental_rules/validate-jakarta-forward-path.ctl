GIVEN METHOD_CALL $unvalidatedForward WHERE
  name = getRequestDispatcher
  arguments.size = 1
  arguments[0].nodeType != StringLiteral
  arguments[0].code !=~ validate
  arguments[0].code !=~ sanitize
  arguments[0].code !=~ strip

TRANSFORM
  METHOD_CALL validatedForwardPath := io.pixee.security.Jakarta.validateForwardPath($unvalidatedForward.arguments[0])
  $unvalidatedForward.arguments[0] = $validatedForwardPath
  RETURN $unvalidatedForward

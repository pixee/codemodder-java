GIVEN METHOD_CALL $unsafeReadline WHERE
  name = readLine
  arguments.size = 0
  type = java.io.BufferedReader

TRANSFORM
  METHOD_CALL safeReadline := io.pixee.security.SafeIO.safeReadLine($unsafeReadline.scope, 1_000_000)
  RETURN safeReadline

GIVEN METHOD_CALL $insecureCreateProcess WHERE
  name = exec
  type = java.lang.Runtime
  arguments.size != 0

TRANSFORM
  $insecureCreateProcess.arguments.insert($createUrl.scope)
  METHOD_CALL safeCreateProcess := io.pixee.security.SystemCommand.runCommand($insecureCreateProcess.arguments)
  RETURN safeCreateProcess

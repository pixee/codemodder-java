rule pixee:java/harden-process-creation
match
  InstanceMethodCall $c {
    type = java.lang.Runtime
    name = exec
  }

require dependency io.openpixee:java-security-toolkit:1.0.0

replace $c with
  StaticMethodCall {
    type = io.openpixee.security.SystemCommand
    name = run
    args = [$c.context, $c.args[0]]
  }

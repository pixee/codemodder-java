rule pixee:java/harden-process-creation
match
  InstanceMethodCall $c {
    type = java.lang.Runtime
    name = exec
  }

replace $c with
  StaticMethodCall {
    type = io.pixee.security.SystemCommand
    name = run
    args = [$c.context, $c.args[0]]
  }

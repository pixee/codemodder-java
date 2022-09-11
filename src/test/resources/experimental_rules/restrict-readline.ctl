rule pixee:java/restrict-readline
match
  InstanceMethodCall $c {
    type = java.io.BufferedReader
    name = readLine
    args = []
  }

require dependency io.pixee:io.pixee.security:1.0
require import io.pixee.security.BoundedLineReader

replace $c with
  StaticMethodCall {
    type = io.pixee.security.BoundedLineReader
    name = readLine
    args = [$c, 1_000_000]
  }

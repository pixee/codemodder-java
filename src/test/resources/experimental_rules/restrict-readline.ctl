rule pixee:java/restrict-readline
match
  InstanceMethodCall $c {
    type = java.io.BufferedReader
    name = readLine
    args = []
  }

require dependency io.openpixee:java-security-toolkit:1.0.0
require import io.openpixee.security.BoundedLineReader

replace $c with
  StaticMethodCall {
    type = io.openpixee.security.BoundedLineReader
    name = readLine
    args = [$c, 1_000_000]
  }

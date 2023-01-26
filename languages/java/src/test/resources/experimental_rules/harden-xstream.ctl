rule pixee:java/harden-xstream
match
  ConstructorCall $c {
    type = com.xstream.XStream
  }
where
  !hasDownstream(
    $c,
    InstanceMethodCall {
      type = com.xstream.XStream
      name = registerConverter,
      context = $c
    }
  )
require dependency io.openpixee:java-security-toolkit-xstream:1.0.0
require import io.openpixee.security.HardeningConverter
insert into dataflow downstream of $c
  StaticMethodCall {
    type = io.openpixee.security.SafeXStream
    name = preventDangerousTypes
    args = [$c]
  }

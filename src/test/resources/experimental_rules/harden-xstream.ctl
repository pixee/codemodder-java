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
require dependency io.pixee:io.pixee.security.xstream:1.1
require import io.pixee.security.SafeXStream
insert into dataflow downstream of $c
  StaticMethodCall {
    type = io.pixee.security.SafeXStream
    name = preventDangerousTypes
    args = [$c]
  }

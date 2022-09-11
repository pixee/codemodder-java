rule pixee:java/harden-xmldecoder-stream
match
  ConstructorCall $c {
    type = java.beans.XMLDecoder
    args = [*:java.io.InputStream, *]
  }
where
  !hasDownstream(
    $c.args[0],
    MethodCall {
      name = harden,
      context = $c.args[0]
    }
  )

require dependency io.pixee:io.pixee.security:1.0
require import io.pixee.security.XMLDecoderSecurity

insert into dataflow downstream of $c
  StaticMethodCall {
    type = io.pixee.security.XMLDecoderSecurity
    name = hardenStream
    args = [$c]
  }

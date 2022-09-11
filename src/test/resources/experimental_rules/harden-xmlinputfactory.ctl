rule pixee:java/harden-xmlinputfactory
match
  StaticMethodCall $c {
    type = javax.xml.stream.XMLInputFactory
    name in (newFactory, newInstance)
    args = [] # really want to limit this to arguments.size = 0 or 2 only
  }
where
  !hasDownstream(
    $c,
    StaticMethodCall {
      name ~ harden,
      context = $c
    }
  )

require dependency io.pixee:io.pixee.security:1.0
require import io.pixee.security.XMLInputFactorySecurity

insert into dataflow downstream of $c
  StaticMethodCall {
    type = io.pixee.security.XMLInputFactorySecurity
    name = hardenFactory
    args = [$c]
  }

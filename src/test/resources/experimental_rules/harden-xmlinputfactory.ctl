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

require dependency io.openpixee:java-security-toolkit:1.0.0
require import io.openpixee.security.XMLInputFactorySecurity

insert into dataflow downstream of $c
  StaticMethodCall {
    type = io.openpixee.security.XMLInputFactorySecurity
    name = hardenFactory
    args = [$c]
  }

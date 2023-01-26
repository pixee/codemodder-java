rule pixee:java/harden-java-deserialization
match
   InstanceMethodCall $c {
     type = java.io.ObjectInputStream
     name = readObject
   }
with
  $objectInputStream = valueOf(c.context)
where
  !hasUpstream(
    $objectInputStream,
    InstanceMethodCall {
      type = io.openpixee.security.ObjectInputFilter
      name = enableFilter
      args = [$objectInputStream]
    }
  )
  &&
  !hasUpstream(
      $objectInputStream,
      StaticMethodCall {
        type = io.openpixee.security.ObjectInputFilters
        name = enableFilter
        args = [$objectInputStream]
      }
    )
)

require dependency io.openpixee:java-security-toolkit:1.0.0
require import io.openpixee.security.ObjectInputFilters

insert into dataflow upstream of $c
  StaticMethodCall {
    type = io.openpixee.security.ObjectInputFilters
    name = enableFilter
    args = [$c.context]
  }

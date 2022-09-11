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
      type = io.pixee.security.ObjectInputFilter
      name = enableFilter
      args = [$objectInputStream]
    }
  )
  &&
  !hasUpstream(
      $objectInputStream,
      StaticMethodCall {
        type = io.pixee.security.ObjectInputFilters
        name = enableFilter
        args = [$objectInputStream]
      }
    )
)

require dependency io.pixee:io.pixee.security:1.0
require import io.pixee.security.ObjectInputFilters

insert into dataflow upstream of $c
  StaticMethodCall {
    type = io.pixee.security.ObjectInputFilters
    name = enableFilter
    args = [$c.context]
  }

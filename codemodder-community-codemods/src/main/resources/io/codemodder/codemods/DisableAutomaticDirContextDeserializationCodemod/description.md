This change hardens Java deserialization operations against attack. Even a simple operation like an object deserialization is an opportunity to yield control of your system to an attacker. In fact, without specific, non-default protections, any object deserialization call can lead to arbitrary code execution. The JavaDoc [now even says](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/ObjectInputFilter.html):

> Deserialization of untrusted data is inherently dangerous and should be avoided.

Let's discuss the attack. In Java, types can customize how they should be deserialized by specifying a `readObject()` method like this real example from an [old version of Spring](https://github.com/spring-projects/spring-framework/blob/4.0.x/spring-core/src/main/java/org/springframework/core/SerializableTypeWrapper.java#L404):

```java
static class MethodInvokeTypeProvider implements TypeProvider {
    private final TypeProvider provider;
    private final String methodName;

    private void readObject(ObjectInputStream inputStream) {
        inputStream.defaultReadObject();
        Method method = ReflectionUtils.findMethod(
                this.provider.getType().getClass(),
                this.methodName
        );
        this.result = ReflectionUtils.invokeMethod(method,this.provider.getType());
    }
}
```

Reflecting on this code reveals a terrifying conclusion. If an attacker presents this object to be deserialized by your app, the runtime will take a class and a method name from the attacker and then call them. Note that an attacker can provide any serliazed type -- it doesn't have to be the one you're expecting, and it will still deserialize.

Attackers can repurpose the logic of selected types within the Java classpath (called "gadgets") and chain them together to achieve arbitrary remote code execution. There are a limited number of publicly known gadgets that can be used for attack, and our change simply inserts an [ObjectInputFilter](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/io/ObjectInputStream.html#setObjectInputFilter(java.io.ObjectInputFilter)) into the `ObjectInputStream` to prevent them from being used.

```diff
+import io.github.pixee.security.ObjectInputFilters;
ObjectInputStream ois = new ObjectInputStream(is);
+ObjectInputFilters.enableObjectFilterIfUnprotected(ois);
AcmeObject acme = (AcmeObject)ois.readObject();
```

This is a tough vulnerability class to understand, but it is deadly serious because it's the highest impact possible (remote code execution) and extremely likely (automated tooling can exploit.) It's best to remove deserialization but our protections will protect you from all known exploitation strategies.

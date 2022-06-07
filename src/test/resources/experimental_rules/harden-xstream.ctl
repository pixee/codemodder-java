ruleId = harden-stream
version = 0.6

GIVEN METHOD_CALL $unsafeXStreamCall WHERE
  name = <init>
  arguments.size = 0
  type = com.thoughtworks.xstream.XStream
  !isRightSideOfSimpleVariableDeclarationStatement()
  !scope.hasMemberMethodCalls("registerConverter")

TRANSFORM
  var templateCode = """
    %SCOPE%.registerConverter(new com.thoughtworks.xstream.converters.Converter() {
      public boolean canConvert(final Class type) {
        return type != null && (type == java.beans.EventHandler.class || type == java.lang.ProcessBuilder.class || java.lang.reflect.Proxy.isProxyClass(type));
      }
      public Object unmarshal(final com.thoughtworks.xstream.io.HierarchicalStreamReader reader, final com.thoughtworks.xstream.converters.UnmarshallingContext context) {
        throw new SecurityException("unsupported type due to security reasons");
      }

      public void marshal(final Object source, final com.thoughtworks.xstream.io.HierarchicalStreamWriter writer, final com.thoughtworks.xstream.converters.MarshallingContext context) {
        throw new SecurityException("unsupported type due to security reasons");
      }
    }, XStream.PRIORITY_LOW);
  """
  var hardeningCode = replace(templateCode, "%SCOPE", $unsafeXStreamCall.scope)
  change_cursor(NEXT_STATEMENT)
  insert_naked_code(hardeningCode)

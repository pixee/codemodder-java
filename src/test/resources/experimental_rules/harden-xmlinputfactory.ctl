GIVEN METHOD_CALL $insecureXmlFactory WHERE
  name = newFactory OR name = newInstance
  type = javax.xml.stream.XMLInputFactory
  arguments.size = 0 OR arguments.size = 2
  enclosingStatement.code !=~ /hardenXmlInputFactory/
TRANSFORM
  METHOD_CALL secureFactoryCall := io.pixee.security.XXE.hardenXmlInputFactory($insecureXmlFactory)
  return secureFactoryCall

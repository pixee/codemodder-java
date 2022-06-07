GIVEN METHOD_CALL $insecureXmlDecoder WHERE
  name = <init>
  type = java.beans.XMLDecoder
  arguments.size != 0
  arguments[0].type = java.io.InputStream

TRANSFORM
  METHOD_CALL secureStreamCall := io.pixee.security.SafeIO.toSafeXmlDecoderInputStream($insecureXmlDecoder.arguments[0])
  $insecureXmlDecoder.arguments[0] := secureStreamCall

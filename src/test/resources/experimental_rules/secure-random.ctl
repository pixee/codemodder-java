GIVEN METHOD_CALL $getInsecure WHERE
  name = <init>
  type = java.util.Random

TRANSFORM
  METHOD_CALL secureRandom := java.security.SecureRandom.<init>($getInsecure.arguments)
  RETURN secureRandom

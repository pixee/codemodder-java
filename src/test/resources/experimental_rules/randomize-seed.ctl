GIVEN METHOD_CALL $constantSeed WHERE
  name = setSeed
  arguments.size = 1
  type = java.util.Random OR java.security.SecureRandom
  arguments[0].nodeType = ConstantExpression

TRANSFORM
  $constantSeed.arguments[0] = System.currentTimeMills()
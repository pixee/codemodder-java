rule pixee:java/make-prng-seed-unpredictable
match
  InstanceMethodCall $c {
    type = java.util.Random
    name = setSeed
    args = [NumericConstant]
  }
replace $c.args[0] with
  StaticMethodCall {
    type = java.lang.System
    name = currentTimeMillis
    args = []
  }
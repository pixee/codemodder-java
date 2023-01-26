rule pixee:java/secure-random
match
   ConstructorCall $c {
       type = java.util.Random
       args = []  
   }
replace $c with
   ConstructorCall {
       type = java.security.SecureRandom
       args = []
   }
report "We changed $c.type $line $file"
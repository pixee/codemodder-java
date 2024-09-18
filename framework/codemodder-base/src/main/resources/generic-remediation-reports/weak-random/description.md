This change replaces all new instances of `java.util.Random` with the marginally slower, but much more secure `java.security.SecureRandom`.

We have to work pretty hard to get computers to generate genuinely unguessable random bits. The `java.util.Random` type uses a method of pseudo-random number generation that unfortunately emits fairly predictable numbers.

If the numbers it emits are predictable, then it's obviously not safe to use in cryptographic operations, file name creation, token construction, password generation, and anything else that's related to security. In fact, it may affect security even if it's not directly obvious.

Switching to a more secure version is simple and our changes all look something like this:

```diff
- Random r = new Random();
+ Random r = new java.security.SecureRandom();
```

This change replaces all the constant seeds passed to `Random#setSeed(long)` with a pseudo-random value, which will make it considerably more secure.

A "seed" tells your pseudo-random number generator (PRNG) "where to start" in a deterministic (huge, but deterministic) set of numbers. If attackers can detect you're using a constant seed, they'll quickly be able to predict the next numbers you will generate.

Our change replaces the constant with `System#currentTimeMillis()`.

```diff
Random random = new Random();
-random.setSeed(123);
+random.setSeed(System.currentTimeMillis());
```

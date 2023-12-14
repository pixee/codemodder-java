TODO

Our changes look something like this:

```diff

-     int leftOver = (int) (((bitCount >>> 3)) & 0x3f);
+     int leftOver = (int) ((bitCount >>> 3) & 0x3f);

```

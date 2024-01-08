This change removes redundant parentheses. These extra parentheses make it harder to understand the code.

Our changes look something like this:

```diff

-     int leftOver = (int) (((bitCount >>> 3)) & 0x3f);
+     int leftOver = (int) ((bitCount >>> 3) & 0x3f);

```

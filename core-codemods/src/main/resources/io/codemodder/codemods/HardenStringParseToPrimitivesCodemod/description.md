This change updates `String`-to-number conversions by leveraging the intended parse methods.

This change makes developer intent clearer, and sometimes with a more concise expression.

Our changes look like this:

```diff
    String number = "7.1";

-   int integerNum = Integer.valueOf(number);
+   int integerNum = Integer.parseInt(number);

-   float floatNumVal = Float.valueOf(number).floatValue();
+   float floatNumVal = Float.parseFloat(number);

-   int integerNumber = new Integer(number);
+   int integerNumber = Integer.parseInt(number);
```

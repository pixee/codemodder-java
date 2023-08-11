This change adds clarifying braces to misleading code blocks that look like they may be executing unintended code.

Consider the following code:
```java
if (isAdmin)
  doFirstThing();
  doSecondThing();
```

Although the code formatting makes it look like `doSecondThing()` only executes if `isAdmin` is true, it actually executes regardless of the value of the condition. This pattern of not having curly braces in combination with misaligned indentation leads to security bugs, including the famous [Apple iOS goto fail bug](https://www.synopsys.com/blogs/software-security/understanding-apple-goto-fail-vulnerability-2/) from their SSL library which allowed attackers to intercept and modify encrypted traffic.

This codemod will add braces to control flow statements to make the code more clear, but only in situations in which there is confusing formatting. Our changes look something like this:
```diff
- if (isAdmin)
+ if (isAdmin) {
    doFirstThing();
+ }    
    doSecondThing();
```

Note that these changes illuminate situations in which there may be bugs and help make the control flow more clear. 

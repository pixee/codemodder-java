This change removes intermediate variables who are only created to be thrown or returned in the next statement. This makes the code more readable, which makes reviewing the code for issues easier.

Our changes look something like this:

```diff
    public LocaleResolver localeResolver() { 
-       SessionLocaleResolver localeResolver = new SessionLocaleResolver();
-       return localeResolver;
+       return new SessionLocaleResolver();
    }
```

```diff
    public void process() { 
-       Exception ex = new Exception();
-       throw ex;
+       throw new Exception();
    }
```

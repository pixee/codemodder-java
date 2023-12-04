TODO

Our changes look something like this:

```diff
    public LocaleResolver localeResolver() { 
-       SessionLocaleResolver localeResolver = new SessionLocaleResolver();
-       return localeResolver;
+       return new SessionLocaleResolver();
    }
```

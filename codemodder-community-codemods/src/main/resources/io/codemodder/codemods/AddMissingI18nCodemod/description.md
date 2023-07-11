This change adds missing keys in the resource property files.

Primarily, missing keys lead to missing translations and blank strings in UIs, which can lead to a poor user experience. However, we have seen cases where missing translations caused exceptional code paths to be executed, leading to information leaks and other more serious vulnerability classes.

Our changes look something like this:

```properties
  login.title=Login
  login.username=Username
+ login.password=Password
```

To provide the translation, we fed other languages' value(s) for this key observed in sibling files to the AWS Translation API. It should be noted that the shorter the value is the more susceptible it is to mis-translation, so it may be worth verifying short values.

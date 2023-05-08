This change ensures that `SSLContext#getInstance()` uses a safe version of Transport Layer Security (TLS), which is necessary for safe SSL connections.

TLS v1.0 and TLS v1.1 both have serious issues and are considered unsafe. Right now, the only safe version to use is 1.2.

Our change involves modifying the arguments to `getInstance()` to return TLSv1.2 when it can be confirmed to be another, less secure value:

```diff
- SSLContext sslContext = SSLContext.getInstance("TLSv1.1");
+ SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
```

There is no functional difference between the unsafe and safe versions, and all modern servers offer TLSv1.2.

This change creates a dynamically formatted SQL query.

Our changes look something like this:

```diff
-   org.hibernate.Query query = session.createQuery("FROM users where username = " + user.getUsername());
+   org.hibernate.Query query =  session.createQuery("FROM users where username = ?");
+   query = query.setParameter(0,user.getUsername());
```

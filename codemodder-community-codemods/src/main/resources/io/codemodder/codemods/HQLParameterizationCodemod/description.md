This change refactors Hibernate queries to be parameterized, rather than built by hand.

Without parameterization, developers must remember to escape inputs using the rules for that database. It's usually buggy, at the least -- and sometimes vulnerable.

Our changes look something like this:

```diff
- Query<User> hqlQuery = session.createQuery("select p from Person p where p.name like '" + tainted + "'");
+ Query<User> hqlQuery = session.createQuery("select p from Person p where p.name like :parameter0").setParameter(":parameter0", tainted);
```

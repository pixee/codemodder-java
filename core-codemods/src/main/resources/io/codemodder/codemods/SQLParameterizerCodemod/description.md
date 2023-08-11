This change refactors SQL statements to be parameterized, rather than built by hand.

Without parameterization, developers must remember to escape inputs using the rules for that database. It's usually buggy, at the least -- and sometimes vulnerable.

Our changes look something like this:

```diff
- Statement stmt = connection.createStatement();
- ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE name = '" + user + "'");
+ PreparedStatement stmt = connection.prepareStatement();
+ stmt.setString(1, user);
+ ResultSet rs = stmt.executeQuery();
```

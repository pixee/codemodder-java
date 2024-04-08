This change fixes SQL injections found in DefectDojo by parameterizing the given statement.

Without parameterization, developers must remember to escape inputs using the rules for that database. It's usually buggy, at the least -- and sometimes vulnerable.

Our changes look something like this:

```diff
- Statement stmt = connection.createStatement();
- ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE name = '" + user + "'");
+ PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE name = ?");
+ stmt.setString(1, user);
+ ResultSet rs = stmt.executeQuery();
```

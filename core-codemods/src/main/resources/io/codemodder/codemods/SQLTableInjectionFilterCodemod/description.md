This change adds a filtering step to any table name inputs in SQL queries. 

The filtering added is an alphanumeric blacklist and should cover common avenues of attacks. However, it is quite 
restrictive and may break applications that uses table names with uncommon characters. Moreover, it is also not 
foolproof. Ideally this filtering step is done as a domain-dependent whitelist that contains all the allowed table 
names.

Our changes look something like this:

```diff
+ void filterTable(final String tablename){
+ 	  var regex = Pattern.compile("[a-zA-Z0-9]+(.[a-zA-Z0-9]+)?");
+ 	  if (!regex.matcher(tablename).matches()){
+ 		  throw new RuntimeException("Supplied table name contains non-alphanumeric characters");
+ 	  }
+ }
...
Statement stmt = connection.createStatement();
- ResultSet rs = stmt.executeQuery("SELECT * FROM" + table_name); 
+ ResultSet rs = stmt.executeQuery("SELECT * FROM" + filterTable(table_name)); 
```

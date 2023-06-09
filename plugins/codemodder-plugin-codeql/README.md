# Codemodder CodeQL Plugin

This plugin makes it possible to write codemods that act on vulnerabilities discovered by [CodeQL](https://codeql.github.com/).

## Creating Sarif Files For Testing

This session addresses how to create Sarif files for codemods arising from the CodeQl plugin.

CodeQL codemods will expect a Sarif file containing the location of the vulnerabilities found by CodeQL labeled by their ID. Tests are no exception.

The `CodemodTestMixin` class expects a `Test.java.before` and `Test.java.after` files. It works by copying and renaming the `.before` file to a temporary directory. It will automatically detect any `.sarif` files in the same directory as the `.before` file and pass it along the `CodemodLoader`.

We create the `.sarif` file for the test by using the `codeql` command line tool. You can find [here](https://docs.github.com/en/code-security/codeql-cli/using-the-codeql-cli/getting-started-with-the-codeql-cli) on how to set up `codeql` cli. 

The `codeql` cli will expect a project. You can quickly create a project with `Maven`:

```bash
mvn archetype:generate -DgroupId=com.mycompany.app -DartifactId=my-app -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4 -DinteractiveMode=false
```

Now create a `Test.java` file in the project containing the test code. Be mindful that you should include any necessary dependencies in the POM file. The `codeql` cli will try to build the project, so `mvn compile` needs to be successful. 

The next step is to create the database for the project. 

```bash
codeql database create java-database --overwrite --language=java
```

You can create the Sarif file by running:

```bash
codeql database analyze java-database --format='sarifv2.1.0' --output='./out.sarif' <path-to-rule-file>
```

Where `<path-to-rule-file>` is the path to the `.ql` file containing the query for the desired rule-id. You can find the file associated with the rule-id by looking at the CodeQL help page for the id. See [here](https://codeql.github.com/codeql-query-help/java/).

This will generate the `out.sarif` file at the current directory. Lastly, you need to update the `Test.java` path in the generated Sarif. If you used the Maven archetype as before, the full path for `Test.java` will be `src/main/java/com/mycompany/app/Test.java`. You need to delete the prefix. Just replace every instance of `src/main/java/com/mycompany/app/Test.java` to `Test.java` in `out.sarif`.

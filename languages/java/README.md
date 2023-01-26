[![Actions Status](https://github.com/pixeeworks/java-code-hardener/workflows/Java%20CI/badge.svg)](https://github.com/pixeeworks/java-code-hardener/actions)
![Coverage](.github/badges/jacoco.svg)

# OpenPixee: Java Language Provider

A utility for generating a list of suggested weaves.

## How to use

### How to configure

TODO: We don't have configuration yet. There are lots of possible configurations:
* Should we try to inject a dependency and call that from our weaves, or just inline hardening?
* Should there be configuration to turn off rules individually?
* Should we inject comments that explain the security implications of this change?

### Running the tool
You should hopefully be running this as part of a polyglot CLI TBD but if you need to run the JAR directly, you can do as follows:

```shell
$ java -jar target/java-code-hardener-X.jar <args>
```

Argument information here:
```
Usage: java-code-hardener [-hvV] [-d=<ruleDefault>] -o=<output> -r=<repositoryRoot>
                 [-e=<excludes>]... [-i=<includes>]... [-s=<sarifs>]...
                 [-x=<ruleExceptions>]...
scans a repository with suggested weaves for Java
  -d, --rule-default=<ruleDefault>
                             Specify the default rule setting ('enabled' or
                               'disabled')
  -e, --exclude=<excludes>   Specify the paths to exclude within the repository
  -h, --help                 Show this help message and exit.
  -i, --include=<includes>   Specify the paths to include within the repository
  -o, --output=<output>      Specify the file to write the output results to
  -r, --repository=<repositoryRoot>
                             Source code repository path
  -s, --sarif=<sarifs>       Specify the paths to SARIFs that the hardener
                               should act on
  -v, --verbose              Specify whether debug logging should be enabled
  -V, --version              Print version information and exit.
  -x, --rule-exception=<ruleExceptions>
                             Specify the rules that should have have the
                               opposite of the default rule setting

```

### Consuming the output
The tool spits out results in the form of a JSON file whose path is passed in a command line argument. The file contains 
suggested weaves that could harden the application. It also contains other diagnostic data.

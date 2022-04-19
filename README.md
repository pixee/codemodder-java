[![Actions Status](https://github.com/pixeeworks/java-code-hardener/workflows/Java%20CI/badge.svg)](https://github.com/dayzerozero/java-fixit/actions)
![Coverage](.github/badges/jacoco.svg)

# java-fixit

A utility for generating a list of suggested weaves.

## How to use

### How to configure

TODO: We don't have configuration yet. There are lots of possible configurations:
* Should we try to inject a dependency and call that from our weaves, or just inline hardening?
* Should there be configuration to turn off rules individually?
* Should we inject comments that explain the security implications of this change?

### Running the tool

```
Usage: pixee-java-hardener [-hV] -c=<configuration> -o=<output> -r=<repositoryRoot>
scans a repository with suggested weaves for Java
-c, --config=<configuration>  Specify the configuration file
-h, --help              Show this help message and exit.
-o, --output=<output>   Specify the file to write the output results to
-r, --repository=<repositoryRoot> Source code repository path
```

### Consuming the output
The tool spits out results in the form of a JSON file whose path is passed in a command line argument. The file contains 
suggested weaves that could harden the application. It also contains other diagnostic data.

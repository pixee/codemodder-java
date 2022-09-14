# Code Transformation Language (CodeTL)

This is the home of Code Transformation Language (CodeTL). The purpose of CodeTL is to allow a wide audience to automatically scale a variety of transformations on their codebases.

## How to set up environment
1. Copy `settings_example.xml` to `settings.xml`. Replace `<ARTIFACTORY_USER>` and `<ARTIFACTORY_TOKEN>` with username and token from jFrog account on `settings.xml` file.

2. Install packages and dependencies
```
mvn -s settings.xml clean install
```

## How to update DSL
1. Download antlr from `https://www.antlr.org/download/antlr-4.7.2-complete.jar`.

2. Go to `src/main/java/io/dsl/java` folder and run the commands below.
```
java -Xmx500M -cp <path>/antlr-4.7.2-complete.jar org.antlr.v4.Tool DSL.g4
```
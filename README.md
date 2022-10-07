# Code Transformation Language (CodeTL)

This is the home of Code Transformation Language -- CodeTL! The purpose of [CodeTL](http://github.com/codetl-spec) is to allow a wide audience to automatically scale a variety of transformations on their custom codebases.
 
Although it is a language, it is also the name of a CLI program which implements the language and performs the transformations.

# Installing

```bash 
# Grab the package
$ npm install @openpixee/codetl # not actually done yet

# Scan and suggest changes to harden my code!
$ codetl --repository=/path/to/my_repo --output=/tmp/my_repo.codetf
```

# Developing
As a developer, you need to install GraalVM (Community Edition 20.3.4) in order to build, test and run locally.
```bash
export JAVA_HARDENER_LIB=../java-code-hardener/target/hardener.jar 
$GRAALVM/bin/npm run build
$GRAALVM/bin/npm run test
$GRAALVM/bin/npm start -- --repository=/tmp/foo --output=/tmp/bar
```

# Supported Languages
The `codetl` program only works on the following languages:
 - Java
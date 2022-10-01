# Code Transformation Language (CodeTL)

This is the home of Code Transformation Language -- CodeTL! The purpose of [CodeTL](http://github.com/codetl-spec) is to allow a wide audience to automatically scale a variety of transformations on their custom codebases.
 
Although it is a language, it is also the name of a CLI program which implements the language and performs the transformations.

# Installing

```bash 
$ npm install codetl # TODO
```

# Building & Testing
As a developer, you need to install GraalVM in order to build, test and run locally.
```bash
$GRAALVM/bin/npm run build
$GRAALVM/bin/npm run test
$GRAALVM/bin/node --jvm --vm.cp=/path/to/java-code-hardener-<VERSION>.jar --polyglot out/entry.js --repository=/tmp/foo --output=/tmp/bar
```

# Supported Languages
The `codetl` program only works on the following languages:
 - Java
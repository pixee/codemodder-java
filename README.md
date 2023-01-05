# codetl

CLI tool for leveraging the Code Transformation Language (CodeTL) to apply complex source code
transformations to projects.

## ⚗️ Language Provider Proof of Concept

CodeTL is a language agnostic tool and code transformation domain specific language (DSL). Support
for a language is added by an extension called a "language provider".

The CodeTL tool is extended with _language providers_. In the working architecture, language
providers are written in the language they support. For example, the JavaScript language provider is
largely written in TypeScript. This proof of concept demonstrates how to integrate language
providers into one process where interoperability between languages is provided by GraalVM.

The proof of concept application demonstrates reading JavaScript source files in Java then using
GraalVM's polyglot SDK to transform those source files with transformations written in TypeScript. A
Gradle build ties all the projects together and produces a binary using GraalVM's native-image tool.

* The JavaScript transformation logic is written in TypeScript in the
  subproject `languages/javascript`. The TypeScript compiler generates ES5 compatible JavaScript
  that uses the CommonJS module system.
* The JavaScript module `module-provider.ts` is the API exposed by the JavaScript language
  provider's TypeScript code to the Java code. As such, this module takes care not to depend on any
  modules that are incompatible when running in a binary produced by GraalVM's `native-image` tool.
* Browserify creates a bundle from `module-provider.ts`. The Gradle build in `languages/javascript`
  shells out to `npm` to create this bundle, and the Gradle build includes the built bundle as a
  _artifact_ so that other Gradle subprojects may depend on it.
* The `cli` subproject's Gradle build script copies the bundle built by the `languages/javascript`
  into the GraalVM native image.
* The Java code in the `cli` subproject reads the JavaScript bundle from the classpath and evaluates
  it in using the GraalVM polyglot SDK. It then reads each JavaScript source file in the given
  repository and transforms it using the transformation function exposed by the bundle.

## Building

1. Install GraalVM 22.3.0
2. Install native-image component
   ```shell
    gu install native-image
   ```
3. Build CLI tool
   ```shell
   ./gradlew assemble
   ```
4. The built binary is at path `./cli/build/native/nativeCompile/codetl`
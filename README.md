<picture>
  <source media="(prefers-color-scheme: dark)" srcset="img/codemodder-dark.png">
  <source media="(prefers-color-scheme: light)" srcset="img/codemodder-light.png">
  <img alt="Pixee Logo" src="https://github.com/pixee/pixee-cli/raw/main/img/codemodder.png">
</picture>

# codemodder-java

This is the Java version of the [Codemodder Framework](https://codemodder.io/), which builds on traditional codemod frameworks by providing
codemods with additional context and services. Codemodder plugins inject codemods with the context and services they need to perform complex transforms.

Pluggability and the complex transforms they enable distinguish codemodder codemods from traditional codemods.

For directions on how to write your own codemods, visit the [Codemodder documentation site](https://codemodder.io/).

Codemodder is sponsored by [pixee.ai](https://pixee.ai).

## Developing

Follow these instructions if you intend to modify and build this project from source.

### First Time Set Up

1. Install JDK 17 for building this project. We recommend [Eclipse Adoptium](https://adoptium.net/)

1. Install [Semgrep](https://semgrep.dev/) CLI. See
   [here](https://semgrep.dev/docs/getting-started/#installing-and-running-semgrep-locally)
   for instructions. It can usually be done via `pip`:
   ```shell
   pip install semgrep
   ```

If your python library paths contain your home directory as a root folder (i.e.
due to the use of the `$HOME` environment variable), you may need to manually
set up your `PYTHONPATH` for tests:

```shell
PYTHONPATH=$HOME/<subpath-to-python-libs-folder> ./gradlew check
```

You can check your python paths with:

```shell
python -m site
```

### Building

```shell
./gradlew assemble
```

### Running Tests

```shell
./gradlew check
```

You need to be able to run [Docker](https://www.docker.com/) for some tests. Make sure you have it installed and have the necessary permissions to run with your user.

### Run the Core Codemods
You can download and run the latest release from this repository in order to run the core codemods as a CLI:

```bash
$ unzip codemodder-java-codemods-<VERSION>.zip
$ sh core-codemods-<VERSION>/bin/core-codemods
```

In order to run them during active development from Gradle, you can use the `run` task:

```shell
./gradlew :core-codemods:run --args='--output /path/to/my.codetf --dry-run /path/to/project/'
```

# Codemodder

The Codemodder framework builds on traditional codemod frameworks by providing
codemods with additional context and services. Codemodder plugins inject
codemods with the context and services they need to perform complex transforms.
Plugins and the complex transforms they enable distinguish codemodder codemods
from traditional codemods.

## Developing

Follow these instructions if you intend to modify and build this project from
source.

### First Time Set Up

1. Configure JFrog Artifactory Authentication by adding the following properties
   to your Gradle home gradle.properties (typically
   `$HOME/.gradle/gradle.properties`):
   ```
   pixeeArtifactoryUsername=<your-username>
   pixeeArtifactoryPassword=<your-token>
   ```
2. Install [Semgrep](https://semgrep.dev/) CLI. See
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

### Run the Core Codemods
```shell
./gradlew :core-codemods:run --args='--output /path/to/my.codetf --dry-run /path/to/project/'
```

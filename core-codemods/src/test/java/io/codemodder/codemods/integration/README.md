## Integration test framework

This is a test framework, which purpose is test that each codemod do not break the functionality of an application
where it was performed a code transformation. For each codemod will be a test application with source code that will
be transformed and tested integrally.

### How it works
- An image with required technologies to run the tool is built with different flavors.
- An image per test project is built to copy the app source code and download all the required dependencies used by the app.
- A main Dockerfile will be used to generate two containers, the generated images will use a specific test project image as base and the codemod will be run in just one of them. Finally, we will have two containers running the same application but one of them will have undergone a  code transformation.
- A request to an endpoint in the test application will be performed to verify the functionality have not changed. 

### Running test locally
#### Generate codemodder base image
- Run `gradle distTar` command in the `core-codemods` root directory to generate `.tar` file used in the base image generation.
- Run the `docker build`command in the root of the project.
```
docker build -f core-codemods/src/test/java/io/codemodder/codemods/integration/baseimage/Dockerfile -t codemodder-base:latest .
```

#### Generate test project base image
Run the `docker build`command in the `codemodder-java` root directory, the `CODEMOD_ID` argument must have the value of the ID of the codemod we want to test.
The image tag must be the codemod ID also.
```
docker build -f core-codemods/src/test/java/io/codemodder/codemods/integration/projectimage/Dockerfile --build-arg CODEMOD_ID=move-switch-default-last -t move-switch-default-last .
```

#### Running the integration test for a specific codemod
```
./gradlew :core-codemods:test --tests io.codemodder.codemods.integration.tests.MoveSwitchDefaultCaseLastIntegrationTest
```

### Adding a new integration test
- Add new test project where `codemod` will be performed during the integration test, the project must be named as the codemod ID.
- Create the codemod integration test class and extend `CodemodIntegrationTestMixin` class which will manage the containers creation.
```
public class MoveSwitchDefaultCaseLastIntegrationTest extends CodemodIntegrationTestMixin {}
```

- Annotate the test class with the `IntegrationTestMetadata` annotation that will contain the ID of the codemod being tested and the
  values for the test cases.
```
@IntegrationTestMetadata(
    codemodId = "move-switch-default-last",
    tests = {
      @TestPropertiesMetadata(endpoint = "http://localhost:%s?day=1", expectedResponse = "Monday"),
      @TestPropertiesMetadata(endpoint = "http://localhost:%s?day=2", expectedResponse = "Tuesday")
    })
```

## Integration test framework

This is a test framework, built to test that each codemod does not break the functionality of an application after code transformation. Each codemod should have its own test application with source code that will be transformed and tested integrally.

### How it works
- An image with required technologies to run the tool is built with different flavors.
- An image per test project is built to copy the app source code and download all the required dependencies used by the app.
- A main Dockerfile will be used to generate two containers, the generated images will use a specific test project image as base and the codemod will be run in just one of them. Finally, we will have two containers running the same application but one of them will have undergone a  code transformation.
- A request to an endpoint in the test application will be performed to verify the functionality has not changed. 

### Running test locally
#### Generate codemodder base image
- Run `gradle distTar` command in the `core-codemods` root directory to generate `.tar` file used in the base image generation.
- Run the `docker build`command in the root of the project.
```
docker build -f core-codemods/src/test/java/io/codemodder/codemods/integration/baseimage/Dockerfile -t codemodder-base:latest .
```

#### Generate test project base image
Run the `docker build`command in the `codemodder-java` root directory, the `CODEMOD_ID` argument must have the value of the ID of the codemod we want to test
and the `CODEMODDER_BASE_IMAGE` argument should have the name of the codemodder base image we want to use as name.
The image tag must be the codemod ID also.  
```
docker build -f core-codemods/src/test/java/io/codemodder/codemods/integration/projectimage/Dockerfile --build-arg CODEMOD_ID=move-switch-default-last --build-arg CODEMODDER_BASE_IMAGE=codemodder-base -t move-switch-default-last .
```

#### Running the integration test for a specific codemod
```
./gradlew :core-codemods:test --tests io.codemodder.codemods.integration.tests.MoveSwitchDefaultCaseLastCodemodIntegrationTest
```

## Integration test framework

This is a test framework, which purpose is test that each codemod do not break the functionality of an application
where it was performed a code transformation. For each codemod will be a test application with source code that will
be transformed and tested integrally.

### How it works
- An image with required technologies to run the tool is built with different flavors.
- An image per project is built to copy the app source code and download all the required dependencies used by the app.
- A main Dockerfile will be used to generate two containers, the generated images will use a specific test project image as base and the codemod will be run in just one of them. Finally, we will have two containers running the same application but one of them will have undergone a  code transformation.
- A request to an endpoint in the test application will be performed to verify the functionality have not changed. 

### Running test locally
#### Generate codemodder base image
Run the `build`command in the root of the project.
```
docker build -f core-codemods/src/test/java/io/codemodder/codemods/baseimage/Dockerfile -t codemodder-base:latest .
```

#### Generate test project base image
Run the `build`command in the root of the project, the `CODEMOD_ID` argument must have the value of the ID of the codemod we want to test.
The image tag must be the codemod ID also.
```
docker build -f core-codemods/src/test/java/io/codemodder/codemods/projectimage/Dockerfile --build-arg CODEMOD_ID=move-switch-default-last -t move-switch-default-last .
```

#### Running the integration test for a specific codemod
```
./gradlew :core-codemods:test --tests io.codemodder.codemods.integration.MoveSwitchDefaultCaseLastIntegrationTest
```

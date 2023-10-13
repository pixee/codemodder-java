ARG CODEMODDER_BASE_IMAGE

FROM $CODEMODDER_BASE_IMAGE

ARG CODEMOD_ID

ENV GRADLE_USER_HOME=/gradle-cache

#Copy test project
WORKDIR /test-applications
COPY /core-codemods/src/test/resources/test-applications/$CODEMOD_ID /test-applications/$CODEMOD_ID


# Build application to download required dependencies
WORKDIR /test-applications/$CODEMOD_ID

ENV GRADLE_USER_HOME=/gradle-cache

# Mount a volume for the Gradle cache
VOLUME $GRADLE_USER_HOME

RUN gradle --refresh-dependencies build

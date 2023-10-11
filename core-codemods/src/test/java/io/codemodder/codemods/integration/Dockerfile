ARG CODEMOD_ID

FROM $CODEMOD_ID

ARG RUN_CODEMOD=false
#defining the argument again because the value is unavailable after the first stage ends.
ARG CODEMOD_ID

ENV GRADLE_USER_HOME=/gradle-cache

# Run the tool against the test project
WORKDIR /codemodder-java/core-codemods-0.0.1/bin

# Use the argument to conditionally run tool
RUN if [ "$RUN_CODEMOD" = "true" ]; then \
        ./core-codemods --output ../../../test-applications/$CODEMOD_ID/my.codetf --codemod-include pixee:java/$CODEMOD_ID ../../../test-applications/$CODEMOD_ID; \
    else \
        echo "Tool will not run"; \
    fi

# Generate test application jar
WORKDIR /test-applications/$CODEMOD_ID
RUN gradle bootJar --info

WORKDIR /test-applications/move-switch-default-last/build/libs
CMD ["java", "-jar", "test-app.jar"]

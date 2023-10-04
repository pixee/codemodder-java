FROM codemodder-base

ARG RUN_CODEMOD=false
ARG CODEMOD_ID

#Copy test project
WORKDIR /test-applications
COPY /core-codemods/src/test/resources/test-applications/$CODEMOD_ID /test-applications/$CODEMOD_ID

WORKDIR /codemodder-java/core-codemods-0.0.1/bin

# Use the argument to conditionally run tool
RUN if [ "$RUN_CODEMOD" = "true" ]; then \
        ./core-codemods --output ../../../test-applications/$CODEMOD_ID/my.codetf --codemod-include pixee:java/$CODEMOD_ID ../../../test-applications/$CODEMOD_ID; \
    else \
        echo "Tool will not run"; \
    fi

WORKDIR /test-applications/$CODEMOD_ID

RUN ./gradlew bootJar

WORKDIR /test-applications/move-switch-default-last/build/libs
CMD ["java", "-jar", "move-switch-default-last-0.0.1-SNAPSHOT.jar"]

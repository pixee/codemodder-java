FROM openjdk:17-jdk-slim

RUN apt-get update

# Install Python 3 and pip
RUN apt-get install -y python3 python3-pip

# Update the package list and install wget
RUN apt-get install -y wget unzip

# Install Gradle
ENV GRADLE_VERSION=8.2.1
ENV GRADLE_HOME=/opt/gradle-$GRADLE_VERSION

RUN wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -P /tmp && \
    unzip -q "/tmp/gradle-${GRADLE_VERSION}-bin.zip" -d /opt && \
    rm -rf "/tmp/gradle-${GRADLE_VERSION}-bin.zip"

# Set environment variables
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

# Set Python as the default Python interpreter
RUN ln -s /usr/bin/python3 /usr/bin/python

# Display the installed versions for verification
RUN java -version
RUN python --version
RUN gradle --version

# Install Semgrep using pip
RUN pip install semgrep

# Intall gradle dependencies
WORKDIR /codemodder-java
COPY /core-codemods/src/test/java/io/codemodder/codemods/baseimage/build.gradle.kts /gradle-dependencies/build.gradle.kts

ENV GRADLE_USER_HOME=/gradle-cache

# Mount a volume for the Gradle cache
VOLUME $GRADLE_USER_HOME

WORKDIR /gradle-dependencies
RUN gradle dependencies --info

# Generate tool executable
WORKDIR /codemodder-java
COPY /core-codemods/build/distributions/core-codemods-0.0.1.tar /codemodder-java

RUN tar -xvf /codemodder-java/core-codemods-0.0.1.tar

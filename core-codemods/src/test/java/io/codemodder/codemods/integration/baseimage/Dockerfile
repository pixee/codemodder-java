FROM openjdk:17-jdk-slim

RUN apt-get update

# Install Python 3, pip and wget
RUN apt-get install -y python3 python3-pip wget unzip

# Install Gradle
ENV GRADLE_VERSION=8.2.1
ENV GRADLE_HOME=/opt/gradle-$GRADLE_VERSION

RUN wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -P /tmp && \
    unzip -q "/tmp/gradle-${GRADLE_VERSION}-bin.zip" -d /opt && \
    rm -rf "/tmp/gradle-${GRADLE_VERSION}-bin.zip"

# Set environment variables
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

RUN ln -s /usr/bin/python3 /usr/bin/python && \
    java -version && \
    python --version && \
    gradle --version && \
    python3 -m pip install semgrep==1.15.0

# Generate tool executable
WORKDIR /codemodder-java
RUN mkdir core-codemods
COPY /core-codemods/build/distributions/*.tar /codemodder-java
# Copy only the content of the extracted directory
RUN tar -xvf /codemodder-java/*.tar -C /codemodder-java/core-codemods/ --strip-components=1

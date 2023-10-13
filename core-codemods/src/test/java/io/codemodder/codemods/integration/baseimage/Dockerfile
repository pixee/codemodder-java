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

# Generate tool executable
WORKDIR /codemodder-java
RUN mkdir core-codemods
COPY /core-codemods/build/distributions/*.tar /codemodder-java
# Copy only the content of the extracted directory
RUN tar -xvf /codemodder-java/*.tar -C /codemodder-java/core-codemods/ --strip-components=1

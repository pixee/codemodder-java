# Use a base image with JDK 17 and Python pre-installed
FROM ubuntu:20.04

# Install OpenJDK 17
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk && \
    apt-get clean

# Install Python 3 and pip
RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    apt-get clean

# Update the package list and install wget
RUN apt-get update && apt-get install -y wget unzip && apt-get clean

# Install Gradle
# Install Gradle
ENV GRADLE_VERSION=8.2.1
ENV GRADLE_HOME=/opt/gradle

RUN wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -P /tmp && \
    unzip -q "/tmp/gradle-${GRADLE_VERSION}-bin.zip" -d /opt && \
    rm -rf "/tmp/gradle-${GRADLE_VERSION}-bin.zip"

ENV PATH="${GRADLE_HOME}/bin:${PATH}"

# Set environment variables (optional)
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# Set Python as the default Python interpreter (optional)
RUN ln -s /usr/bin/python3 /usr/bin/python

# Display the installed versions for verification (optional)
RUN java -version
RUN python --version

# Install Semgrep using pip
RUN pip install semgrep


WORKDIR /codemodder-java
COPY /core-codemods/build/distributions/core-codemods-0.0.1.tar /codemodder-java

RUN tar -xvf /codemodder-java/core-codemods-0.0.1.tar

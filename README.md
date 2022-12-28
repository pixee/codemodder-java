## Building

1. Install GraalVM 22.3.0
2. Install native-image component
   ```shell
    gu install native-image
   ```
3. Build CLI tool
   ```shell
   ./gradlew :codetl:cli:assemble
   ```
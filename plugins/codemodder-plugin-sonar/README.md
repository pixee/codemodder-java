This plugin adds support for Sonar results discovered through SonarQube or SonarCloud. 

The plugin works by forwarding a JSON file to the codemods. The JSON file is expected to be the output of the [Sonar Issues API](https://next.sonarqube.com/sonarqube/web_api/api/issues/search). For creating test cases, you may want to bring retrieve a single issue in that JSON. Here is a `curl` command to do just that:

```bash
$ curl -u $SONAR_TOKEN: "https://sonarcloud.io/api/issues/search?projects=<PROJECT_NAME>&statuses=OPEN&ps=500&issues=<ISSUE ID>" | jq .
```

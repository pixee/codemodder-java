package io.codemodder.providers.sarif.semgrep;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SemgrepRunnerTest {

  @Test
  void it_runs_semgrep_and_produces_expected_sarif(@TempDir Path repositoryDir) throws IOException {

    // create vulnerable code in a new temporary repository dir
    Path javaFile = Files.createTempFile(repositoryDir, "WeakRandom", ".java");
    String insecureRandomJavaClass = "class Foo { Random rnd = new Random(); }";
    Files.writeString(javaFile, insecureRandomJavaClass);

    Path ruleFile = Files.createTempFile(repositoryDir, "example", ".yaml");
    InputStream resourceAsStream =
        Objects.requireNonNull(getClass().getResourceAsStream("/example.semgrep"));
    Files.copy(resourceAsStream, ruleFile, StandardCopyOption.REPLACE_EXISTING);

    // run the scan
    SarifSchema210 sarif =
        new DefaultSemgrepRunner().run(List.of(ruleFile), repositoryDir, List.of("**"), List.of());

    // assert the scan went as we think it should
    List<Run> runs = sarif.getRuns();
    assertThat(runs.size(), is(1));
    List<Result> results = runs.get(0).getResults();
    assertThat(results.size(), is(1));
    Result result = results.get(0);
    assertThat(result.getRuleId().endsWith("secure-random"), is(true));
    String resultPhysicalFilePath =
        result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();
    assertThat(resultPhysicalFilePath.contains("WeakRandom"), is(true));
  }
}

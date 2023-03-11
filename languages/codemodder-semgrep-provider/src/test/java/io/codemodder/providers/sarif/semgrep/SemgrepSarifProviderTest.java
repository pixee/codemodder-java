package io.codemodder.providers.sarif.semgrep;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SemgrepSarifProviderTest {

  @Test
  void it_injects_sarif(@TempDir Path repositoryDir) throws IOException {
    Path javaFile = Files.createTempFile(repositoryDir, "WeakRandom", ".java");
    String insecureRandomJavaClass = "class Foo { Random rnd = new Random(); }";
    Files.write(javaFile, insecureRandomJavaClass.getBytes(StandardCharsets.UTF_8));

    Injector injector = Guice.createInjector(new SemgrepSarifModule(repositoryDir));
    UsesSemgrepSarif instance = injector.getInstance(UsesSemgrepSarif.class);
    List<Run> runs = instance.sarif.getRuns();
    assertThat(runs.size(), is(1));
    List<Result> results = runs.get(0).getResults();
    assertThat(results.size(), is(1));
    Result result = results.get(0);
    assertThat(result.getRuleId().endsWith("secure-random"), is(true));
    String resultPhysicalFilePath =
        result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();
    assertThat(resultPhysicalFilePath.contains("WeakRandom"), is(true));
  }

  static class UsesSemgrepSarif {
    private final SarifSchema210 sarif;

    @Inject
    UsesSemgrepSarif(SemgrepSarifProvider sarifProvider) throws IOException, URISyntaxException {
      this.sarif = Objects.requireNonNull(sarifProvider.getSarif("example.semgrep"));
    }
  }

  static class SemgrepSarifModule extends AbstractModule {
    private final Path repositoryDir;

    SemgrepSarifModule(Path repositoryDir) {
      this.repositoryDir = Objects.requireNonNull(repositoryDir);
    }

    @Override
    protected void configure() {
      bind(SemgrepSarifProvider.class).toInstance(new DefaultSemgrepSarifProvider(repositoryDir));
    }
  }
}

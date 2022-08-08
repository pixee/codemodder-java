package io.pixee.codetl.java;

import io.pixee.codefixer.java.ChangedFile;
import io.pixee.codefixer.java.IncludesExcludes;
import io.pixee.codefixer.java.SourceDirectory;
import io.pixee.codefixer.java.SourceWeaver;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.WeavingResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;

class VisitorBasedFactoryModificationTest {
  @TempDir
  Path tempDir;

  @Test
  void checkVisitorFactoryCreation() throws IOException {
    var dsl =
        """
           MATCH
             ConsCall $c
               target: "Random"
           REPLACE $c WITH
             ConsCall 
               target: "java.security.SecureRandom"
        """;

    var vulnerableCode = """
            package a.b.c;
            
            import java.util.Random;
                        
            interface RandomVulnerability {
                        
              default void hasThing() {
                Random random = new Random();
                random = new Random(100L); // can't touch this one because SecureRandom has no similar signature
                random = new Random(getLong()); // same
              }
                        
              long getLong();
            }""";
    var expected = """
            package a.b.c;
            
            import java.util.Random;
                        
            interface RandomVulnerability {
                        
              default void hasThing() {
                Random random = new java.security.SecureRandom();
                random = new Random(100L); // can't touch this one because SecureRandom has no similar signature
                random = new Random(getLong()); // same
              }
                        
              long getLong();
            }
            """;

    String actual = analyze(dsl, vulnerableCode);

    assertThat(actual, equalToIgnoringWhiteSpace(expected));
  }

  private String analyze(String dsl, String vulnerableCode) throws IOException {
    SourceDirectory directory = prepareSourceDir(vulnerableCode);

    List<VisitorFactory> visitorFactories = createFactories(dsl);
    WeavingResult result = weave(directory, visitorFactories);

    return convertToSourceCode(result);
  }

  @NotNull
  private List<VisitorFactory> createFactories(String dsl) {
    var processor = new VisitorBasedDSLProcessor();
    var factory = processor.parse(dsl);
    return List.of(factory);
  }

  @NotNull
  private SourceDirectory prepareSourceDir(String vulnerableCode) throws IOException {
    var fullSourcePath = tempDir.toAbsolutePath().toFile().getPath();
    String fileName = convertToFile(vulnerableCode);
    return SourceDirectory.createDefault(
            fullSourcePath,
            List.of(new File(fullSourcePath, fileName).getAbsolutePath())
    );
  }

  private String convertToSourceCode(WeavingResult weave) throws IOException {
    var changedFiles = weave.changedFiles();
    ChangedFile changedFile = changedFiles.iterator().next();

    return Files.readString(Path.of(changedFile.modifiedFile()));
  }

  @NotNull
  private String convertToFile(String vulnerableCode) throws IOException {
    var fileName = "VulnerableRandom.java";
    final Path pathToVulnerableFile = Files.createFile(tempDir.resolve(fileName));

    Files.writeString(pathToVulnerableFile, vulnerableCode);
    return fileName;
  }

  private WeavingResult weave(SourceDirectory directory, List<VisitorFactory> visitorFactories) throws IOException {
    var includesExcludes = IncludesExcludes.fromConfiguration(
            tempDir.toFile(),
            Collections.emptyList(),
            Collections.emptyList());
    return SourceWeaver.createDefault().weave(List.of(directory), visitorFactories, includesExcludes);
  }
}

package io.codemodder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/** Represents a file to which we're suggesting changes. */
public interface ChangedFile {

  /** The absolute path of the file we discovered we wanted to change. */
  @NotNull
  Path originalFilePath();

  /** The absolute path containing our modified version of the file. */
  @NotNull
  Path modifiedFile();

  /** A description of the changes. */
  @NotNull
  List<CodemodChange> changes();

  class Default implements ChangedFile {
    private final Path javaFile;
    private final Path backupFile;
    private final List<CodemodChange> weaves;

    Default(final Path javaFile, final Path modifiedFile, final List<CodemodChange> weaves) {
      this.javaFile = Objects.requireNonNull(javaFile, "javaFile");
      this.backupFile = Objects.requireNonNull(modifiedFile, "modifiedFile");
      this.weaves = Collections.unmodifiableList(Objects.requireNonNull(weaves, "weaves"));
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Default aDefault = (Default) o;
      return javaFile.equals(aDefault.javaFile)
          && backupFile.equals(aDefault.backupFile)
          && weaves.equals(aDefault.weaves);
    }

    @Override
    public int hashCode() {
      return Objects.hash(javaFile, backupFile, weaves);
    }

    @Override
    public @NotNull Path originalFilePath() {
      return javaFile;
    }

    @Override
    public @NotNull Path modifiedFile() {
      return backupFile;
    }

    @Override
    public @NotNull List<CodemodChange> changes() {
      return weaves;
    }
  }

  static ChangedFile createDefault(
      final Path file, final Path modifiedFile, final CodemodChange weave) {
    return new Default(file, modifiedFile, List.of(weave));
  }

  static ChangedFile createDefault(
      final Path file, final Path modifiedFile, final List<CodemodChange> weaves) {
    return new Default(file, modifiedFile, weaves);
  }
}

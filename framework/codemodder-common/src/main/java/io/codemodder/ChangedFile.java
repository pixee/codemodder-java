package io.codemodder;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/** Represents a file to which we're suggesting changes. */
public interface ChangedFile {

  /** The absolute path of the file we discovered we wanted to change. */
  @NotNull
  String originalFilePath();

  /** The absolute path containing our modified version of the file. */
  @NotNull
  String modifiedFile();

  /** A description of the changes. */
  @NotNull
  List<Weave> weaves();

  class Default implements ChangedFile {
    private final String javaFile;
    private final String modifiedFile;
    private final List<Weave> weaves;

    Default(final String javaFile, final String modifiedFile, final List<Weave> weaves) {
      this.javaFile = Objects.requireNonNull(javaFile, "javaFile");
      this.modifiedFile = Objects.requireNonNull(modifiedFile, "modifiedFile");
      this.weaves = Collections.unmodifiableList(Objects.requireNonNull(weaves, "weaves"));
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Default aDefault = (Default) o;
      return javaFile.equals(aDefault.javaFile)
          && modifiedFile.equals(aDefault.modifiedFile)
          && weaves.equals(aDefault.weaves);
    }

    @Override
    public int hashCode() {
      return Objects.hash(javaFile, modifiedFile, weaves);
    }

    @Override
    public @NotNull String originalFilePath() {
      return javaFile;
    }

    @Override
    public @NotNull String modifiedFile() {
      return modifiedFile;
    }

    @Override
    public @NotNull List<Weave> weaves() {
      return weaves;
    }
  }

  static ChangedFile createDefault(
      final String file, final String modifiedFile, final Weave weave) {
    return new Default(file, modifiedFile, List.of(weave));
  }

  static ChangedFile createDefault(
      final String file, final String modifiedFile, final List<Weave> weaves) {
    return new Default(file, modifiedFile, weaves);
  }
}

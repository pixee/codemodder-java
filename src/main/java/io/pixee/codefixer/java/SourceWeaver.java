package io.pixee.codefixer.java;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.universalchardet.UniversalDetector;

/** A visitor that finds opportunities for changes/protections/hardening. */
public interface SourceWeaver {

  /** Go through the given source directories and return a {@link WeavingResult}. */
  WeavingResult weave(
      List<SourceDirectory> javaSourceDirectories,
      List<VisitorFactory> visitorFactories,
      IncludesExcludes includesExcludes)
      throws IOException;

  static SourceWeaver createDefault() {
    return new DefaultSourceWeaver();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation uses com.github.javaparser library as it seemed to "do the best it can"
   * in the face of missing information, as well as some cool streaming APIs. We may be able to find
   * better libraries.
   */
  final class DefaultSourceWeaver implements SourceWeaver {

    @Override
    public @NotNull WeavingResult weave(
        final List<SourceDirectory> javaSourceDirectories,
        final List<VisitorFactory> visitorFactories,
        final IncludesExcludes includesExcludes)
        throws IOException {
      /*
       * Create the parser which can resolve symbols across all the Java source directories.
       */
      final JavaParser javaParser = createJavaParser(javaSourceDirectories);
      final Set<ChangedFile> changedFiles = new HashSet<>();
      final Set<String> unscannableFiles = new HashSet<>();
      int filesScanned = 0;

      final long totalFiles =
          javaSourceDirectories.stream()
              .map(SourceDirectory::files)
              .flatMap(Collection::stream)
              .count();

      LOG.info("Files to scan: {}", totalFiles);
      for (SourceDirectory sourceDirectory : javaSourceDirectories) {
        List<String> javaFiles =
            sourceDirectory.files().stream()
                .filter(file -> includesExcludes.shouldInspect(new File(file)))
                .collect(Collectors.toList());

        for (String javaFile : javaFiles) {
          filesScanned++;
          if ((filesScanned % 250) == 0) {
            LOG.info("Scanned {} files", filesScanned);
          }
          try {
            final ChangedFile changedFile =
                scanIndividualJavaFile(javaParser, javaFile, visitorFactories, includesExcludes);
            if (changedFile != null) {
              changedFiles.add(changedFile);
            }
          } catch (UnparseableFileException e) {
            LOG.error("Problem parsing file {}", javaFile, e);
            unscannableFiles.add(javaFile);
          }
        }
      }

      return new WeavingResult.Default(changedFiles, unscannableFiles);
    }

    private static class UnparseableFileException extends Exception {
      private UnparseableFileException(final String javaFile) {
        super(javaFile);
      }
    }

    /** Scan the file. */
    @Nullable
    private ChangedFile scanIndividualJavaFile(
        final JavaParser javaParser,
        final String javaFile,
        final List<VisitorFactory> visitorFactories,
        final IncludesExcludes includesExcludes)
        throws IOException, UnparseableFileException {
      final File file = new File(javaFile).getCanonicalFile();
      final InputStream in = new FileInputStream(file);
      final ParseResult<CompilationUnit> result = javaParser.parse(in);
      if (!result.isSuccessful()) {
        throw new UnparseableFileException(javaFile);
      }

      final CompilationUnit cu = result.getResult().orElseThrow();
      LexicalPreservingPrinter.setup(cu);
      return scanType(file, cu, visitorFactories, includesExcludes);
    }

    /** For each type in a Java source file, we scan through the code. */
    private ChangedFile scanType(
        final File javaFile,
        final CompilationUnit cu,
        final List<VisitorFactory> visitorFactories,
        final IncludesExcludes includesExcludes)
        throws IOException {

      final FileWeavingContext context =
          new FileWeavingContext.Default(
              javaFile, includesExcludes.getIncludesExcludesForFile(javaFile));

      visitorFactories.forEach(
          vf -> {
            final ModifierVisitor<FileWeavingContext> visitor =
                vf.createJavaCodeVisitorFor(javaFile, cu);
            cu.accept(visitor, context);
          });

      if (context.madeWeaves()) {
        final String encoding = detectEncoding(javaFile);
        final String modified = (LexicalPreservingPrinter.print(cu));

        File modifiedFile = File.createTempFile(javaFile.getName(), ".java");
        FileUtils.write(modifiedFile, modified, encoding);

        return new ChangedFile.Default(
            javaFile.getAbsolutePath(), modifiedFile.getAbsolutePath(), context.weaves());
      }

      return null;
    }

    /**
     * Try to detect the encoding of the original file to write back, defaulting to UTF-8 if none
     * can be detected.
     */
    private String detectEncoding(final File originalJavaFile) throws IOException {
      String encoding = UniversalDetector.detectCharset(originalJavaFile);
      if (encoding == null) {
        encoding = "UTF-8";
      }
      return encoding;
    }

    @NotNull
    private JavaParser createJavaParser(final List<SourceDirectory> javaSourceDirectories) {
      final JavaParser javaParser = new JavaParser();
      final CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
      combinedTypeSolver.add(new ReflectionTypeSolver());

      javaSourceDirectories.forEach(
          javaDirectory -> combinedTypeSolver.add(new JavaParserTypeSolver(javaDirectory.path())));

      javaParser
          .getParserConfiguration()
          .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
      return javaParser;
    }

    private static final Logger LOG = LogManager.getLogger(DefaultSourceWeaver.class);
  }
}

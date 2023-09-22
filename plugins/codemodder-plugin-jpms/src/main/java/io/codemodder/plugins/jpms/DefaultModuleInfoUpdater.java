package io.codemodder.plugins.jpms;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultModuleInfoUpdater implements ModuleInfoUpdater {

  @Override
  public DependencyUpdateResult update(
      final Path projectDir,
      final Path moduleInfoJava,
      final Path fileBeingChanged,
      final List<DependencyGAV> remainingFileDependencies)
      throws IOException {
    JavaParser javaParser = new JavaParser();
    ParserConfiguration parserConfiguration = javaParser.getParserConfiguration();
    parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
    ParseResult<CompilationUnit> parse = javaParser.parse(moduleInfoJava);
    if (!parse.isSuccessful()) {
      log.debug("Couldn't parse compilation unit to insert module definition: {}", moduleInfoJava);
      return DependencyUpdateResult.EMPTY_UPDATE;
    }
    Optional<CompilationUnit> parseResult = parse.getResult();
    if (parseResult.isEmpty()) {
      log.debug(
          "Empty compilation unit when trying to update module definition: {}", moduleInfoJava);
      return DependencyUpdateResult.EMPTY_UPDATE;
    }
    CompilationUnit cu = parseResult.get();
    LexicalPreservingPrinter.setup(cu);

    Optional<ModuleDeclaration> module = cu.getModule();
    if (module.isEmpty()) {
      log.warn("Couldn't find module definition: {}", moduleInfoJava);
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    String before = cu.toString();
    ModuleDeclaration moduleDeclaration = module.get();

    boolean changed = false;
    List<DependencyGAV> injectedDependencies = new ArrayList<>();
    List<DependencyGAV> skippedDependencies = new ArrayList<>();
    for (DependencyGAV dependency : remainingFileDependencies) {
      if (dependency.moduleName().isPresent()) {
        ModuleRequiresDirective directive = new ModuleRequiresDirective();
        directive.setName(dependency.moduleName().get());
        moduleDeclaration.addDirective(directive);
        injectedDependencies.add(dependency);
        changed = true;
      } else {
        skippedDependencies.add(dependency);
      }
    }

    String after = cu.toString();
    if (!changed) {
      log.debug("No changes to module-info.java: {}", moduleInfoJava);
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    log.debug("Updating module-info.java: {}", moduleInfoJava);
    Files.writeString(moduleInfoJava, after);

    String relativePath = projectDir.relativize(moduleInfoJava).toString();
    List<String> beforeLines = Files.readAllLines(moduleInfoJava);
    List<String> afterLines = after.lines().toList();
    Patch<String> patch = DiffUtils.diff(before.lines().toList(), afterLines);
    List<String> patchDiff =
        UnifiedDiffUtils.generateUnifiedDiff(relativePath, relativePath, beforeLines, patch, 3);

    String diffString = String.join("\n", patchDiff);
    int position = 1 + patch.getDeltas().get(0).getSource().getPosition();
    CodeTFChange change = new CodeTFChange(position, Map.of(), "", List.of(), null, List.of());
    CodeTFChangesetEntry entry =
        new CodeTFChangesetEntry(relativePath, diffString, List.of(change));
    return DependencyUpdateResult.create(
        injectedDependencies, skippedDependencies, List.of(entry), Set.of());
  }

  private static final Logger log = LoggerFactory.getLogger(DefaultModuleInfoUpdater.class);
}

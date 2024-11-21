package io.codemodder.providers.sarif.codeql;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.RuleSarif;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for distributing the SARIFS to CodeQL based codemods based on rules. */
public final class CodeQLModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final List<RuleSarif> allCodeqlRuleSarifs;

  CodeQLModule(
      final List<Class<? extends CodeChanger>> codemodTypes, final List<RuleSarif> sarifs) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.allCodeqlRuleSarifs = sarifs;
  }

  @Override
  protected void configure() {
    // What if there are multiple sarif files with a given rule?
    // We can safely ignore this case for now.
    final Map<String, RuleSarif> map = new HashMap<>();
    allCodeqlRuleSarifs.forEach(
        rs -> {
          if (!map.containsKey(rs.getRule())) {
            map.put(rs.getRule(), rs);
          } else {
            log.warn(
                "Multiple SARIFs found for rule: {}, ignoring results after first", rs.getRule());
          }
        });

    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      final Constructor<?>[] constructors = codemodType.getDeclaredConstructors();

      final Optional<ProvidedCodeQLScan> annotation =
          Stream.of(constructors)
              .filter(constructor -> constructor.getAnnotation(javax.inject.Inject.class) != null)
              .flatMap(constructor -> Stream.of(constructor.getParameters()))
              .map(parameter -> parameter.getAnnotation(ProvidedCodeQLScan.class))
              .filter(Objects::nonNull)
              .findFirst();

      annotation.ifPresent(
          providedCodeQLScan ->
              bind(RuleSarif.class)
                  .annotatedWith(providedCodeQLScan)
                  .toInstance(map.getOrDefault(providedCodeQLScan.ruleId(), RuleSarif.EMPTY)));
    }
  }

  private static final Logger log = LoggerFactory.getLogger(CodeQLModule.class);
}

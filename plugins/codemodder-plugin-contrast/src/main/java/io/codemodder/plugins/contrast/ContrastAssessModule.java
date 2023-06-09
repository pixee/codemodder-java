package io.codemodder.plugins.contrast;

import com.contrastsecurity.sarif.SarifSchema210;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.RuleSarif;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.*;

import static java.util.stream.Collectors.toList;

final class ContrastAssessModule extends AbstractModule {

    private final Path repository;
    private final List<Class<? extends CodeChanger>> codemodTypes;
    private final ContrastSarifTransformer transformer;

    ContrastAssessModule(final Path repository, final List<Class<? extends CodeChanger>> codemodTypes) {
        this.repository = Objects.requireNonNull(repository);
        this.codemodTypes = Objects.requireNonNull(codemodTypes);
        this.transformer = ContrastSarifTransformer.create();
    }

    @Override
    protected void configure() {
        // find all @ContrastAssessSnapshot annotations in their parameters and batch them up for running
        List<Pair<String, ContrastAssessSnapshot>> toBind = new ArrayList<>();
        Set<String> packagesScanned = new HashSet<>();
        for (Class<? extends CodeChanger> codemodType : codemodTypes) {

            String packageName = codemodType.getPackageName();
            if (!packagesScanned.contains(packageName)) {
                ScanResult scan =
                        new ClassGraph().enableAllInfo().acceptPackagesNonRecursive(packageName).scan();
                ClassInfoList classesWithMethodAnnotation =
                        scan.getClassesWithMethodAnnotation(Inject.class);
                List<Class<?>> injectableClasses = classesWithMethodAnnotation.loadClasses();

                List<Parameter> targetedParams =
                        injectableClasses.stream()
                                .map(Class::getDeclaredConstructors)
                                .flatMap(Arrays::stream)
                                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                                .map(Executable::getParameters)
                                .flatMap(Arrays::stream)
                                .filter(param -> param.isAnnotationPresent(ContrastAssessSnapshot.class))
                                .collect(toList());

                targetedParams.forEach(
                        param -> {
                            if (!RuleSarif.class.equals(param.getType())) {
                                throw new IllegalArgumentException(
                                        "can't use @ContrastAssessSnapshot on anything except RuleSarif (see "
                                                + param.getDeclaringExecutable().getDeclaringClass().getName()
                                                + ")");
                            }

                            ContrastAssessSnapshot contrastAssessSnapshot = param.getAnnotation(ContrastAssessSnapshot.class);
                            String ruleId = contrastAssessSnapshot.ruleId();
                            Pair<String, ContrastAssessSnapshot> rulePair = Pair.of(ruleId, contrastAssessSnapshot);

                            toBind.add(rulePair);
                        });

                LOG.debug("Finished scanning codemod package: {}", packageName);
                packagesScanned.add(packageName);
            }
        }

        if (toBind.isEmpty()) {
            // no reason to run semgrep if there are no annotations
            return;
        }

        // actually run the SARIF only once
        SarifSchema210 sarif;
        try {
            sarif = getContrastResults();
        } catch (IOException e) {
            throw new IllegalArgumentException("Contrast Assess results retrieval failed", e);
        }

        // bind the SARIF results
        for (Pair<String, ContrastAssessSnapshot> bindingPair : toBind) {
            ContrastAssessSnapshot sarifAnnotation = bindingPair.getRight();
            ContrastAssessRuleSarif contrastAssessSarif = ContrastAssessRuleSarif(bindingPair.getLeft(), sarif);
            bind(RuleSarif.class).annotatedWith(sarifAnnotation).toInstance(contrastAssessSarif);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ContrastAssessModule.class);
}

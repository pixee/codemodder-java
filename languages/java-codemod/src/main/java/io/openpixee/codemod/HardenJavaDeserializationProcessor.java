package io.openpixee.codemod;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtExecutableReference;

import java.io.ObjectInputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Responsible for transform pixee:java/harden-java-deserialization
 */
final class HardenJavaDeserializationProcessor extends AbstractProcessor<CtLocalVariable<ObjectInputStream>> {

    private final Set<Region> locations;

    HardenJavaDeserializationProcessor(final SarifSchema210 sarif) {
        this.locations = sarif.getRuns().get(0).getResults().stream().filter(result -> result.getRuleId().endsWith(".pixee:java/harden-java-deserialization")).map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void process(final CtLocalVariable<ObjectInputStream> localVariable) {
        int line = localVariable.getPosition().getLine();
        int column = localVariable.getType().getPosition().getColumn();
        if(locations.stream().noneMatch(loc -> loc.getStartLine() == line && loc.getStartColumn() == column)) {
            return;
        }
        Factory factory = localVariable.getFactory();
        final CtExpression<?> target = factory.Code().createCodeSnippetExpression("io.openpixee.security.ObjectInputFilters");
        final CtExecutableReference<?> reference = factory.Method().createReference("void io.openpixee.security.ObjectInputFilters#enableObjectFilterIfUnprotected(java.io.ObjectInputStream)");
        final List<CtExpression<?>> arguments = List.of(factory.createCodeSnippetExpression(localVariable.getSimpleName()));
        CtInvocation<?> hardeningStmt = factory.Code().createInvocation(target, reference, arguments);

        localVariable.replace(
                List.of(localVariable, hardeningStmt)
        );
    }
}

package io.pixee.codefixer.java;

import io.pixee.codefixer.java.plugins.codeql.CodeQlPlugin;
import io.pixee.codefixer.java.plugins.contrast.ContrastScanPlugin;
import io.pixee.codefixer.java.protections.DependencyInjectingVisitor;
import io.pixee.codefixer.java.protections.DeserializationVisitorFactory;
import io.pixee.codefixer.java.protections.DeserializationVisitorFactoryNg;
import io.pixee.codefixer.java.protections.HeaderInjectionVisitorFactoryNg;
import io.pixee.codefixer.java.protections.JakartaForwardVisitoryFactoryNg;
import io.pixee.codefixer.java.protections.JspScriptletXSSVisitor;
import io.pixee.codefixer.java.protections.MultipartVisitorFactory;
import io.pixee.codefixer.java.protections.PredictableSeedVisitorFactory;
import io.pixee.codefixer.java.protections.RuntimeExecVisitorFactory;
import io.pixee.codefixer.java.protections.SSLProtocolVisitorFactory;
import io.pixee.codefixer.java.protections.SSRFVisitorFactory;
import io.pixee.codefixer.java.protections.UnsafeReadlineVisitorFactory;
import io.pixee.codefixer.java.protections.VerbTamperingVisitor;
import io.pixee.codefixer.java.protections.WeakPRNGVisitorFactory;
import io.pixee.codefixer.java.protections.XMLDecoderVisitorFactory;
import io.pixee.codefixer.java.protections.XStreamVisitorFactory;
import io.pixee.codefixer.java.protections.XXEVisitorFactory;
import io.pixee.codefixer.java.protections.ZipFileOverwriteVisitoryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Assembles a list of {@link VisitorFactory} we'll need to weave based on configuration.
 */
public interface VisitorAssembler {

    /**
     * Given the context, assemble a list of {@link VisitorFactory} we'll use in our Java code weaving.
     * @param repositoryRoot the root directory of the repository we're weaving
     * @param ruleContext the rules
     * @param sarifs the SARIF files
     * @return the {@link VisitorFactory} types that are allowed to operate
     */
    List<VisitorFactory> assembleJavaCodeScanningVisitorFactories(File repositoryRoot, RuleContext ruleContext, List<File> sarifs);

    List<VisitorFactoryNg> assembleJavaCodeScanningVisitorFactoriesNg(File repositoryRoot, RuleContext ruleContext, List<File> sarifs);

    /**
     * Given the context, assemble of a list of {@link FileBasedVisitor} we'll use in our non-Java code weaving.
     * @param ruleContext the rules
     * @return the {@link FileBasedVisitor} types that are allowed to operate
     */
    List<FileBasedVisitor> assembleFileVisitors(RuleContext ruleContext);

    static VisitorAssembler createDefault() {
        return new Default();
    }

    class Default implements VisitorAssembler {

        @Override
        public List<VisitorFactory> assembleJavaCodeScanningVisitorFactories(final File repositoryRoot, final RuleContext ruleContext, final List<File> sarifs) {
            List<VisitorFactory> defaultVisitorFactories =
                    List.of(
                            new MultipartVisitorFactory(),
                            new RuntimeExecVisitorFactory(),
                            new SSLProtocolVisitorFactory(),
                            new SSRFVisitorFactory(),
                            new UnsafeReadlineVisitorFactory(),
                            new PredictableSeedVisitorFactory(),
                            new WeakPRNGVisitorFactory(),
                            new XMLDecoderVisitorFactory(),
                            new XStreamVisitorFactory(),
                            new XXEVisitorFactory(),
                            new ZipFileOverwriteVisitoryFactory());

            final List<SarifProcessorPlugin> sarifProcessorPlugins =
                    List.of(new CodeQlPlugin(), new ContrastScanPlugin());

            List<VisitorFactory> pluginFactories =
                    new PluginFactoryFinder().getPluginFactories(repositoryRoot, ruleContext, sarifs, sarifProcessorPlugins);

            final List<VisitorFactory> factories = new ArrayList<>();
            factories.addAll(defaultVisitorFactories);
            factories.addAll(pluginFactories);

            LOG.info("Factories available: {}", factories.size());
            factories.removeIf(factory -> !ruleContext.isRuleAllowed(factory.ruleId()));
            LOG.info("Factories after removing disallowed: {}", factories.size());
            return Collections.unmodifiableList(factories);
        }

        @Override
        public List<VisitorFactoryNg> assembleJavaCodeScanningVisitorFactoriesNg(final File repositoryRoot, final RuleContext ruleContext, final List<File> sarifs) {
            List<VisitorFactoryNg> defaultVisitorFactories =
                    List.of(
                            new DeserializationVisitorFactoryNg(),
                            new HeaderInjectionVisitorFactoryNg(),
                            new JakartaForwardVisitoryFactoryNg());

            final List<SarifProcessorPlugin> sarifProcessorPlugins =
                    List.of(new CodeQlPlugin(), new ContrastScanPlugin());

            List<VisitorFactoryNg> pluginFactories =
                    new PluginFactoryFinder().getPluginFactoriesNg(repositoryRoot, ruleContext, sarifs, sarifProcessorPlugins);

            final List<VisitorFactoryNg> factories = new ArrayList<>();
            factories.addAll(defaultVisitorFactories);
            factories.addAll(pluginFactories);

            LOG.info("NgFactories available: {}", factories.size());
            factories.removeIf(factory -> !ruleContext.isRuleAllowed(factory.ruleId()));
            LOG.info("NgFactories after removing disallowed: {}", factories.size());
            return Collections.unmodifiableList(factories);
        }

        @Override
        public List<FileBasedVisitor> assembleFileVisitors(final RuleContext ruleContext) {
            List<FileBasedVisitor> defaultVisitors = new ArrayList<>();
            defaultVisitors.add(new DependencyInjectingVisitor());
            defaultVisitors.add(new JspScriptletXSSVisitor());
            defaultVisitors.add(new VerbTamperingVisitor());
            defaultVisitors.removeIf(visitor -> !ruleContext.isRuleAllowed(visitor.ruleId()));
            return Collections.unmodifiableList(defaultVisitors);
        }
    }

    Logger LOG = LogManager.getLogger(VisitorAssembler.class);
}

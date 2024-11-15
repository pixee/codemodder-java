package io.codemodder.codemods;

import io.codemodder.CodeChanger;
import io.codemodder.Runner;
import io.codemodder.codemods.codeql.*;
import io.codemodder.codemods.semgrep.SemgrepJavaDeserializationCodemod;
import io.codemodder.codemods.semgrep.SemgrepMissingSecureFlagCodemod;
import io.codemodder.codemods.semgrep.SemgrepReflectionInjectionCodemod;
import io.codemodder.codemods.semgrep.SemgrepSQLInjectionCodemod;
import io.codemodder.codemods.semgrep.SemgrepSQLInjectionFormattedSqlStringCodemod;
import io.codemodder.codemods.semgrep.SemgrepSSRFCodemod;
import io.codemodder.codemods.semgrep.SemgrepServletResponseWriterXSSCodemod;
import io.codemodder.codemods.semgrep.SemgrepWeakRandomCodemod;
import io.codemodder.codemods.semgrep.SemgrepXXECodemod;
import java.util.List;

/**
 * Give an ability for users to list all the codemods so they don't have to reference them
 * individually.
 */
public final class DefaultCodemods {

  /** Get a list of all the codemods in our default set. */
  public static List<Class<? extends CodeChanger>> asList() {
    return List.of(
        AddClarifyingBracesCodemod.class,
        AddMissingOverrideCodemod.class,
        AvoidImplicitPublicConstructorCodemod.class,
        CodeQLDeserializationOfUserControlledDataCodemod.class,
        CodeQLHttpResponseSplittingCodemod.class,
        CodeQLInputResourceLeakCodemod.class,
        CodeQLInsecureCookieCodemod.class,
        CodeQLJDBCResourceLeakCodemod.class,
        CodeQLJEXLInjectionCodemod.class,
        CodeQLJNDIInjectionCodemod.class,
        CodeQLMavenSecureURLCodemod.class,
        CodeQLOutputResourceLeakCodemod.class,
        CodeQLPredictableSeedCodemod.class,
        CodeQLRegexInjectionCodemod.class,
        CodeQLSQLInjectionCodemod.class,
        CodeQLSSRFCodemod.class,
        CodeQLStackTraceExposureCodemod.class,
        CodeQLUnverifiedJwtCodemod.class,
        CodeQLXSSCodemod.class,
        CodeQLXXECodemod.class,
        DeclareVariableOnSeparateLineCodemod.class,
        DefectDojoSqlInjectionCodemod.class,
        DefineConstantForLiteralCodemod.class,
        DisableAutomaticDirContextDeserializationCodemod.class,
        FixRedundantStaticOnEnumCodemod.class,
        HardenJavaDeserializationCodemod.class,
        HardenStringParseToPrimitivesCodemod.class,
        HardenProcessCreationCodemod.class,
        HardenXMLDecoderCodemod.class,
        HardenXMLInputFactoryCodemod.class,
        HardenXMLReaderCodemod.class,
        HardenXStreamCodemod.class,
        HardenZipEntryPathsCodemod.class,
        HQLParameterizationCodemod.class,
        JSPScriptletXSSCodemod.class,
        LimitReadlineCodemod.class,
        OverridesMatchParentSynchronizationCodemod.class,
        PreventFileWriterLeakWithFilesCodemod.class,
        RandomizeSeedCodemod.class,
        RemoveRedundantVariableCreationCodemod.class,
        RemoveUnusedImportCodemod.class,
        RemoveUnusedLocalVariableCodemod.class,
        RemoveUnusedPrivateMethodCodemod.class,
        RemoveUselessParenthesesCodemod.class,
        ReplaceDefaultHttpClientCodemod.class,
        ReplaceStreamCollectorsToListCodemod.class,
        // ResourceLeakCodemod.class,
        SanitizeApacheMultipartFilenameCodemod.class,
        SanitizeHttpHeaderCodemod.class,
        SanitizeSpringMultipartFilenameCodemod.class,
        SecureRandomCodemod.class,
        SemgrepJavaDeserializationCodemod.class,
        SemgrepMissingSecureFlagCodemod.class,
        SemgrepReflectionInjectionCodemod.class,
        SemgrepServletResponseWriterXSSCodemod.class,
        SemgrepSSRFCodemod.class,
        SemgrepSQLInjectionCodemod.class,
        SemgrepSQLInjectionFormattedSqlStringCodemod.class,
        SemgrepWeakRandomCodemod.class,
        SemgrepXXECodemod.class,
        SemgrepOverlyPermissiveFilePermissionsCodemod.class,
        SimplifyRestControllerAnnotationsCodemod.class,
        SubstituteReplaceAllCodemod.class,
        SonarJNDIInjectionCodemod.class,
        SonarObjectDeserializationCodemod.class,
        SonarRemoveUnthrowableExceptionCodemod.class,
        SonarSQLInjectionCodemod.class,
        SonarSSRFCodemod.class,
        SonarUnsafeReflectionRemediationCodemod.class,
        SonarXXECodemod.class,
        SQLParameterizerCodemod.class,
        SSRFCodemod.class,
        SwitchLiteralFirstComparisonsCodemod.class,
        SwitchToStandardCharsetsCodemod.class,
        UpgradeSSLContextTLSCodemod.class,
        UpgradeSSLEngineTLSCodemod.class,
        UpgradeSSLParametersTLSCodemod.class,
        UpgradeSSLSocketProtocolsTLSCodemod.class,
        UpgradeTempFileToNIOCodemod.class,
        UseEmptyForToArrayCodemod.class,
        ValidateJakartaForwardPathCodemod.class,
        VerboseRequestMappingCodemod.class,
        VerbTamperingCodemod.class);
  }

  /**
   * Entry point for core codemods.
   *
   * @param args the arguments to pass to the runner
   */
  public static void main(final String[] args) {
    Runner.run(asList(), args);
  }
}

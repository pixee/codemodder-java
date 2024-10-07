package io.codemodder;

import com.contrastsecurity.sarif.SarifSchema210;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** A context that is passed to a {@link RemediationCodemod}. */
public interface RemediationContext {

    /** The project directory to be analyzed/fixed. */
    CodeDirectory directory();

    Optional<SarifSchema210> findSarifByVendor(String vendorName);

    List<Path> sonarHotspotPaths();

    List<Path> sonarIssuePaths();

    List<Path> contrastAssessPaths();
}

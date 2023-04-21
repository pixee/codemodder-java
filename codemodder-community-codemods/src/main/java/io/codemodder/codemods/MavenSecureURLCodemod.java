package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import io.codemodder.*;
import io.codemodder.providers.sarif.codeql.CodeQLScan;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/** Fixes issues reported under the id "java/maven-non-https-url". */
@Codemod(
        id = "codeql:java/maven/non-https-url",
        author = "andre.silva@pixee.ai",
        reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public class MavenSecureURLCodemod extends SarifPluginRawFileChanger {

    @Inject
    MavenSecureURLCodemod(
            @CodeQLScan(ruleId = "java/maven/non-https-url") final RuleSarif sarif) {
        super(sarif);
    }

    @Override
    public List<Weave> onFileFound(final CodemodInvocationContext context, final List<Result> results) {
        return List.of();
    }
}

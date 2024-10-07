package io.codemodder;

import io.codemodder.codetf.CodeTFReference;
import io.codemodder.codetf.CodeTFResult;

import java.util.List;

/** A codemod that is meant to be given a result, find and modify the right files, and return a completed {@link CodeTFResult}. */
public interface RemediationCodemod {

    /** Run the codemod */
    CodeTFResult run(RemediationContext context);

    /** Codemod ID, like "semgrep:java/xss". */
    String getId();

    /** References for more reading about this codemod. */
    List<CodeTFReference> getReferences();
}

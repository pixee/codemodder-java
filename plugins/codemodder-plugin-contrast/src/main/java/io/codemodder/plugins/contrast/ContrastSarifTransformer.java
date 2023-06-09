package io.codemodder.plugins.contrast;

/**
 * A type responsible for transforming Contrast Assess results into a SARIF format.
 */
interface ContrastSarifTransformer {


    static ContrastSarifTransformer create() {
        return null;
    }
}

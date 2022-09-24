/**
 * Represents the CodeTF format
 * @see https://github.com/openpixee/codetf
 */
export interface CodeTF {
    foo: string
}

/**
 * Responsible for aggregating and normalizing multiple CodeTF files into one. Each individual language provider
 * provides their own CodeTF, but our CLI is only expected to produce one.
 */
export interface CodeTFAggregator {
    aggregate(codetfs: string[]) : CodeTF
}

/**
 * Responsible for reporting the CodeTF to the output file.
 */
export interface CodeTFReporter {
    report(codetf: CodeTF, outputFile : string) : void
}

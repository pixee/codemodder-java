/**
 * Represents the CodeTF format
 * @see https://github.com/openpixee/codetf
 */
export interface CodeTF {
    run: CodeTFRun
    results: CodeTFResult[]
}

export interface CodeTFRun {
    vendor: string,
    tool: string,
    commandLine: string,
    elapsed: number,
    configuration: CodeTFConfiguration,
    fileExtensionsScanned: CodeTFFileExtensionScanned[]
}

export interface CodeTFFileExtensionScanned {
    extension: string,
    count: number
}

export interface CodeTFResult {
    path: string,
    diff: string,
    changes: CodeTFChange[]
}

export interface CodeTFChange {
    lineNumber: number,
    category: string,
    description: string,
    properties: Map<any,any>
}

export interface CodeTFConfiguration {
    directory: string,
    inputs: CodeTFInput,
    modules: string[],
    includes: string[],
    excludes: string[]
}

export interface CodeTFInput {
    artifact: string,
    sha1: string,
    vendor: string
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

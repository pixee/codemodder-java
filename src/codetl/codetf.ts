/**
 * Represents the CodeTF format
 * @see https://github.com/openpixee/codetf
 */
export class CodeTF {
    readonly run: CodeTFRun
    readonly results: CodeTFResult[]
}

export class CodeTFRun {
    readonly vendor: string
    readonly tool: string
    readonly commandLine: string
    readonly elapsed: number
    readonly configuration: CodeTFConfiguration
    readonly fileExtensionsScanned: CodeTFFileExtensionScanned[]
}

export class CodeTFFileExtensionScanned {
    readonly extension: string
    readonly count: number
}

export class CodeTFResult {
    readonly path: string
    readonly diff: string
    readonly changes: CodeTFChange[]
}

export class CodeTFChange {
    readonly lineNumber: number
    readonly category: string
    readonly description: string
    readonly properties: Map<string,any>
}

export class CodeTFConfiguration {
    readonly directory: string
    readonly inputs: CodeTFInput
    readonly modules: string[]
    readonly includes: string[]
    readonly excludes: string[]
}

export class CodeTFInput {
    readonly artifact: string
    readonly sha1: string
    readonly vendor: string
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

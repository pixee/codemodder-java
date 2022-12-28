/**
 * Represents the CodeTF format
 * @see https://github.com/openpixee/codetf
 */
import {CodeTLExecutionContext} from "./cli";
import {RequiredDependency} from "../../../codetl-parser/src/codetl/parser";

export class CodeTF {
    public readonly run: CodeTFRun;
    public readonly results: CodeTFResult[];

    constructor(run : CodeTFRun, results : CodeTFResult[]) {
        this.run = run;
        this.results = results;
    }
}

export class CodeTFRun {
    public readonly vendor : string;
    public readonly tool : string;
    public readonly commandLine : string;
    public readonly elapsed : number;
    public readonly configuration : CodeTFConfiguration;
    public readonly fileExtensionsScanned : CodeTFFileExtensionScanned[];

    constructor(vendor: string, tool : string, commandLine : string, elapsed : number, configuration : CodeTFConfiguration, fileExtensionsScanned : CodeTFFileExtensionScanned[]) {
        this.vendor = vendor;
        this.tool = tool
        this.commandLine = commandLine
        this.elapsed = elapsed
        this.configuration = configuration
        this.fileExtensionsScanned = fileExtensionsScanned
    }
}

export class CodeTFFileExtensionScanned {
    public readonly extension: string;
    public readonly count: number;

    constructor(extension : string, count : number) {
        this.extension = extension
        this.count = count
    }
}

export class CodeTFResult {
    public readonly path: string;
    public readonly diff: string;
    public readonly changes: CodeTFChange[];

    constructor(path : string, diff : string, changes : CodeTFChange[]) {
        this.path = path
        this.diff = diff
        this.changes = changes
    }
}

export class CodeTFChange {

    public readonly lineNumber: number;
    public readonly category: string;
    public readonly description: string;
    public readonly properties: Map<string, any>;

    constructor(lineNumber : number, category : string, description : string, properties : Map<string,any>) {
        this.lineNumber = lineNumber;
        this.category = category;
        this.description = description;
        this.properties = properties;
    }
}

export class CodeTFConfiguration {

    public readonly directory: string;
    public readonly inputs: CodeTFInput[];
    public readonly modules: string[];
    public readonly includes : string[];
    public readonly excludes: string[];

    constructor(directory : string, inputs : CodeTFInput[], modules : string[], includes : string[], excludes : string[]) {
        this.directory = directory
        this.inputs = inputs
        this.modules = modules
        this.includes = includes
        this.excludes = excludes
    }
}

export class CodeTFInput {
    public readonly artifact: string;
    public readonly sha256: string;
    public readonly tool: string;

    constructor(artifact : string, sha256 : string, tool : string) {
        this.artifact = artifact
        this.sha256 = sha256
        this.tool = tool
    }
}

/**
 * Responsible for aggregating and normalizing multiple CodeTF files into one. Each individual language provider
 * provides their own CodeTF, but our CLI is only expected to produce one.
 */
export interface CodeTFAggregator {
    aggregate(executionContext : CodeTLExecutionContext, codetfs: LanguageProviderResult[], elapsed : number) : CodeTF
}

/**
 * Responsible for reporting the CodeTF to the output file.
 */
export interface CodeTFReporter {
    report(codetf: CodeTF, outputFile : string) : void
}

/**
 * This type is not part of the CodeTF spec, but is used internally to collect individual CodeTF results from each
 * subject language module.
 */
export class LanguageProviderResult {
    public readonly vendor: string;
    public readonly language: string;
    public readonly results: CodeTFResult[];
    public readonly newDependencies: RequiredDependency[];

    constructor(vendor : string, language : string, results : CodeTFResult[], newDependencies : RequiredDependency[]) {
        this.vendor = vendor
        this.language = language
        this.results = results
        this.newDependencies = newDependencies
    }
}

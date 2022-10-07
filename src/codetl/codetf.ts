/**
 * Represents the CodeTF format
 * @see https://github.com/openpixee/codetf
 */
import {CodeTLExecutionContext} from "./cli";
import {RequiredDependency} from "../../../codetl-parser/src/codetl/parser";

export class CodeTF {
    private readonly _run: CodeTFRun;
    private readonly _results: CodeTFResult[];

    constructor(run : CodeTFRun, results : CodeTFResult[]) {
        this._run = run;
        this._results = results;
    }

    public run(): CodeTFRun {
        return this._run;
    }

    public results(): CodeTFResult[] {
        return this._results;
    }
}

export class CodeTFRun {
    private readonly _vendor : string;
    private readonly _tool : string;
    private readonly _commandLine : string;
    private readonly _elapsed : number;
    private readonly _configuration : CodeTFConfiguration;
    private readonly _fileExtensionsScanned : CodeTFFileExtensionScanned[];

    constructor(vendor: string, tool : string, commandLine : string, elapsed : number, configuration : CodeTFConfiguration, fileExtensionsScanned : CodeTFFileExtensionScanned[]) {
        this._vendor = vendor;
        this._tool = tool
        this._commandLine = commandLine
        this._elapsed = elapsed
        this._configuration = configuration
        this._fileExtensionsScanned = fileExtensionsScanned
    }

    public vendor(): string {return this._vendor}
    public tool(): string {return this._tool}
    public commandLine(): string {return this._commandLine}
    public elapsed(): number { return this._elapsed}
    public configuration(): CodeTFConfiguration {return this._configuration}
    public fileExtensionsScanned(): CodeTFFileExtensionScanned[] {return this._fileExtensionsScanned}
}

export class CodeTFFileExtensionScanned {
    private readonly _extension: string;
    private readonly _count: number;

    constructor(extension : string, count : number) {
        this._extension = extension
        this._count = count
    }
    public extension(): string { return this._extension}
    public count(): number { return this._count}
}

export class CodeTFResult {
    private readonly _path: string;
    private readonly _diff: string;
    private readonly _changes: CodeTFChange[];

    constructor(path : string, diff : string, changes : CodeTFChange[]) {
        this._path = path
        this._diff = diff
        this._changes = changes
    }
    public path(): string {return this._path}
    public diff(): string { return this._diff}
    public changes(): CodeTFChange[] { return this._changes;}
}

export class CodeTFChange {

    private readonly _lineNumber: number;
    private readonly _category: string;
    private readonly _description: string;
    private readonly _properties: Map<string, any>;

    constructor(lineNumber : number, category : string, description : string, properties : Map<string,any>) {
        this._lineNumber = lineNumber;
        this._category = category;
        this._description = description;
        this._properties = properties;
    }

    public lineNumber(): number {return this._lineNumber}
    public category(): string {return this._category}
    public description(): string { return this._description }
    public properties(): Map<string,any> {return this._properties}
}

export class CodeTFConfiguration {
    private _directory: string;
    private _inputs: CodeTFInput[];
    private _includes: string[];
    private _modules: string[];
    private _excludes: string[];
    constructor(directory : string, inputs : CodeTFInput[], modules : string[], includes : string[], excludes : string[]) {
        this._directory = directory
        this._inputs = inputs
        this._modules = modules
        this._includes = includes
        this._excludes = excludes
    }

    public directory(): string {return this._directory}
    public inputs(): CodeTFInput[] {return this._inputs}
    public modules(): string[] {return this._modules}
    public includes(): string[] {return this._includes}
    public excludes(): string[]{return this._excludes}
}

export class CodeTFInput {
    private readonly _artifact: string;
    private readonly _sha256: string;
    private readonly _tool: string;
    constructor(artifact : string, sha256 : string, tool : string) {
        this._artifact = artifact
        this._sha256 = sha256
        this._tool = tool
    }
    public artifact(): string {return this._artifact}
    public sha1(): string {return this._sha256}
    public tool(): string {return this._tool}
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
    private readonly _vendor: string;
    private readonly _language: string;
    private readonly _changes: CodeTFResult[];
    private readonly _newDependencies: RequiredDependency[];

    constructor(vendor : string, language : string, changes : CodeTFResult[], newDependencies : RequiredDependency[]) {
        this._vendor = vendor
        this._language = language
        this._changes = changes
        this._newDependencies = newDependencies
    }

    public vendor() : string{return this._vendor}
    public language() : string {return this._language}
    public changes() : CodeTFResult[] {return this._changes}
    public newDependencies() : RequiredDependency[] {return this._newDependencies}
}

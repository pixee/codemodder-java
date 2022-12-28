import {LanguageProvider} from "./providers";
import {Log} from "sarif";
import {
  CodeTF,
  CodeTFRun,
  CodeTFFileExtensionScanned,
  CodeTFAggregator,
  CodeTFInput,
  CodeTFResult,
  CodeTFReporter,
  LanguageProviderResult,
  CodeTFConfiguration
} from "./codetf";
import {readFileSync, writeFileSync} from "fs";
import {CodeTLParser} from "../../../codetl-parser/src/codetl/parser";
import {fromConfiguration, IncludesExcludes} from "./includes";
import {createHash} from "crypto";
import {extname} from "path";
import {getAllFilesSync} from "get-all-files";

/**
 * This is the message we send to language providers in order to scan their given code.
 */
export interface CodeTLExecutionContext {
  repository: string,
  ruleDefault: string,
  ruleExceptions: string[],
  sarifFilePaths: string[],
  includes: string[],
  excludes: string[]
}

/**
 * Responsible for execution of the CodeTL process given the arguments.
 */
export interface CodeTLExecutor {
  run(codetlExecutionContext : CodeTLExecutionContext, outputFile: string) : void
}

export class DefaultCodeTLExecutor implements CodeTLExecutor {

  private readonly aggregator: CodeTFAggregator;
  private readonly reporter: CodeTFReporter;
  private readonly languageProviders: LanguageProvider[];

  constructor(aggregator: CodeTFAggregator, reporter: CodeTFReporter, languageProviders : LanguageProvider[]) {
    this.aggregator = aggregator;
    this.reporter = reporter;
    this.languageProviders = languageProviders;
  }

  run(executionContext: CodeTLExecutionContext, outputFile: string): void {

    const rule : string = `
       rule pixee:java/secure-random
       match
          ConstructorCall $c {
             type = "java.util.Random"
          }
       replace $c 
          ConstructorCall {
             type = "java.security.SecureRandom"
          }
       report "Replaced a weak PRNG with a strong one" 
    `;

    const parser = new CodeTLParser();
    const parsedAst = parser.parse(rule);
    if(parsedAst.errors.length != 0) {
      for(let error in parsedAst.errors) {
        console.log(error)
      }
      throw new Error("errors in compilation")
    }

    const ruleDefinition = parser.convertAstToRuleDefinition(parsedAst.ast);
    const includesExcludes : IncludesExcludes = fromConfiguration(executionContext.repository, executionContext.includes, executionContext.excludes)

    let startTime = new Date().getTime()
    const allLanguageProviderResults : LanguageProviderResult[] = []
    this.languageProviders.forEach(function(lp) {
      const result = lp.process(executionContext.repository, includesExcludes, [ruleDefinition]);
      allLanguageProviderResults.push(result)
    });

    let elapsed = new Date().getTime() - startTime
    const aggregateCodetfFile : CodeTF = this.aggregator.aggregate(executionContext, allLanguageProviderResults, elapsed);
    this.reporter.report(aggregateCodetfFile, outputFile);
  }

}

export class DefaultCodeTFAggregator implements CodeTFAggregator {

  aggregate(executionContext : CodeTLExecutionContext, languageProviderResults: LanguageProviderResult[], elapsed : number): CodeTF {
    const allResults : CodeTFResult[] = []
    languageProviderResults.forEach(lpResult => lpResult.results.forEach(lpChange => allResults.push(lpChange)))
    const extensions : CodeTFFileExtensionScanned[] = this.countFileExtensions(executionContext.repository)

    const allModules : string[] = languageProviderResults.map(lpResult => lpResult.vendor + "/" + lpResult.language)
    const allInputs : CodeTFInput[] = executionContext.sarifFilePaths.map(
        sarif => {
          const sarifFileBuffer = readFileSync(sarif).toString();
          const sarifSha = createHash('sha256');
          sarifSha.update(sarifFileBuffer);
          const sha256Hex = sarifSha.digest('hex');
          const sarifLog : Log = JSON.parse(sarifFileBuffer)
          const tools : Set<string> = new Set<string>()
          sarifLog.runs.forEach(run => {
            if(run.tool && run.tool.driver && run.tool.driver.name) {
              const driver = run.tool.driver
              let tool : string = driver.organization ? driver.organization : "unknown"
              tool += "/"
              tool += driver.name
              tool += "/"
              tool += driver.version ? driver.version : "unknown"
              tools.add(tool)
            }
          })
          return new CodeTFInput(sarif, sha256Hex, Array.from(tools.values()).join(","))
        }
    )

    const config : CodeTFConfiguration = new CodeTFConfiguration(process.cwd(), allInputs, allModules, executionContext.includes, executionContext.excludes)

    const run : CodeTFRun = new CodeTFRun("openpixee", "codetl", process.argv.splice(2).join(" "), elapsed, config, extensions)
    return new CodeTF(run, allResults)
  }

  private countFileExtensions(repository: string) : CodeTFFileExtensionScanned[] {
    const extensionScannedMap : Map<string, number> = new Map<string,number>();
    getAllFilesSync(repository, {resolve: true}).toArray().forEach(file => {
      const extension : string = extname(file)
      if(extension) {
        if(extensionScannedMap.has(extension)) {
          extensionScannedMap.set(extension, extensionScannedMap.get(extension)!! + 1)
        } else {
          extensionScannedMap.set(extension, 1)
        }
      }
    });

    const extensions : CodeTFFileExtensionScanned[] = []
    for(let [extension,count] of extensionScannedMap.entries()) {
      extensions.push(new CodeTFFileExtensionScanned(extension, count))
    }

    return extensions;
  }

}

export class DefaultCodeTFReporter implements CodeTFReporter {
  report(codetf: CodeTF, outputFile: string): void {
    writeFileSync(outputFile, JSON.stringify(codetf));
    console.log("Wrote CodeTF file to: " + outputFile)
  }
}
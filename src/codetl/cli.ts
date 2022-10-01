import {LanguageProvider} from "./providers";
import {CodeTF, CodeTFAggregator, CodeTFReporter, CodeTFRun} from "./codetf";
import * as fs from "fs";

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
    const codetfFiles : string[] = []
    this.languageProviders.forEach(function(lp) {
      const codetfFile = lp.process(executionContext);
      codetfFiles.push(codetfFile);
    });

    const aggregateCodetfFile : CodeTF = this.aggregator.aggregate(codetfFiles);
    this.reporter.report(aggregateCodetfFile, outputFile);
  }
}

export class DefaultCodeTFAggregator implements CodeTFAggregator {
  aggregate(codetfs: string[]): CodeTF {
    return new CodeTF();
  }
}

export class DefaultCodeTFReporter implements CodeTFReporter {
  report(codetf: CodeTF, outputFile: string): void {
    fs.writeFileSync(outputFile, JSON.stringify(codetf));
  }
}
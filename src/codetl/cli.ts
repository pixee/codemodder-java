import {LanguageProvider} from "./providers";
import {CodeTF, CodeTFAggregator, CodeTFReporter} from "./codetf";
import * as fs from "fs";
import {CodeTLParser} from "../../../codetl-parser/src/codetl/parser";
import {fromConfiguration, IncludesExcludes} from "./includes";

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

    const rule : string = `
       rule pixee:java/secure-random
       match
          ConstructorCall $c {
             type = java.util.Random
          }
       replace $c 
          ConstructorCall {
             type = java.security.SecureRandom
          }
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
    const matchNode = ruleDefinition.matchNode;
    const name = matchNode.concept!!.name;

    const includesExcludes : IncludesExcludes = fromConfiguration(executionContext.repository, executionContext.includes, executionContext.excludes)

    this.languageProviders.forEach(function(lp) {
      const codetfFile = lp.process(includesExcludes);
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
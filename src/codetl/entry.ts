import {ArgumentParser} from "./args";
import {RuleSetting} from "./rules";
import {CodeTLExecutionContext, DefaultCodeTFAggregator, DefaultCodeTFReporter, DefaultCodeTLExecutor} from "./cli";
import {JavaLanguageProvider} from "./langs/java";

/**
 * Entry point for CLI!
 */
const args = new ArgumentParser().parse(process.argv);

const codetlExecutionContext : CodeTLExecutionContext = {
    repository : args.repository as string,
    ruleDefault : args.ruleDefault as RuleSetting,
    ruleExceptions : args.ruleException != undefined ? args.ruleException : [],
    sarifFilePaths : args.sarifFilePaths != undefined ? args.sarifFilePaths : [],
    includes : args.includes as string[],
    excludes : args.excludes as string[]
};

const executor = new DefaultCodeTLExecutor(
    new DefaultCodeTFAggregator(),
    new DefaultCodeTFReporter(),
    [new JavaLanguageProvider()]
);

executor.run(codetlExecutionContext, args.output);


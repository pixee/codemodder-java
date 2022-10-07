import {ArgumentParser} from "./args";
import {RuleSetting} from "./rules";
import {CodeTLExecutionContext, DefaultCodeTFAggregator, DefaultCodeTFReporter, DefaultCodeTLExecutor} from "./cli";
import {DefaultJavaCodeTLInterpreter, JavaLanguageProvider} from "./langs/java";

/**
 * Entry point for CLI!
 */
const args = new ArgumentParser().parse(process.argv);

const codetlExecutionContext : CodeTLExecutionContext = {
    repository : args.repository as string,
    ruleDefault : args['rule-default'] as RuleSetting,
    ruleExceptions : args['rule-exceptions'] != undefined ? args['rule-exceptions'] as string[] : [],
    sarifFilePaths : args['sarif-file-paths'] != undefined ? args['sarif-file-paths'] as string[] : [],
    includes : args.includes as string[],
    excludes : args.excludes as string[]
};

const executor = new DefaultCodeTLExecutor(
    new DefaultCodeTFAggregator(),
    new DefaultCodeTFReporter(),
    [new JavaLanguageProvider(new DefaultJavaCodeTLInterpreter())]
);

executor.run(codetlExecutionContext, args.output);


import {LanguageProvider} from "../providers";
import {CodeTLExecutionContext} from "../cli";
import tempfile from 'tempfile';

/**
 * This module supports analyzing/transforming Java code
 * @see https://github.com/openpixee/java-code-hardener
 */
export class JavaLanguageProvider implements LanguageProvider {
    process(codetlExecutionContext: CodeTLExecutionContext): string {
        // @ts-ignore
        let javaHardenerEntryType = Java.type('io.pixee.codefixer.java.JavaFixitCli');
        const outputFile = tempfile(".codetf");
        const args : string[] = []
        args.push('--output=' + outputFile)
        args.push('--repository=' + codetlExecutionContext.repository)
        args.push('--rule-default='+ codetlExecutionContext.ruleDefault)

        if(codetlExecutionContext.ruleExceptions != undefined) {
            codetlExecutionContext.ruleExceptions.forEach((exc) => {
                args.push('--rule-exception=' + exc)
            });
        }
        if(codetlExecutionContext.includes != undefined) {
            codetlExecutionContext.includes.forEach((include) => {
                args.push('--include=' + include)
            });
        }

        if(codetlExecutionContext.excludes != undefined) {
            codetlExecutionContext.excludes.forEach((exclude) => {
                args.push('--exclude=' + exclude)
            });
        }

        if(codetlExecutionContext.sarifFilePaths != undefined) {
            codetlExecutionContext.sarifFilePaths.forEach((sarifFilePath) => {
                args.push('--input=' + sarifFilePath)
            });
        }

        javaHardenerEntryType.main(args)

        // the java module will have populated the output file
        return outputFile;
    }
}
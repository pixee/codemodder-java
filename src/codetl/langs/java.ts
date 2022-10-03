import {LanguageProvider} from "../providers";
import tempfile from 'tempfile';
import {IncludesExcludes} from "../includes";

/**
 * This module supports analyzing/transforming Java code
 * @see https://github.com/openpixee/java-code-hardener
 */
export class JavaLanguageProvider implements LanguageProvider {
    process(includesExcludes: IncludesExcludes): string {
        // @ts-ignore
        let javaHardenerEntryType = Java.type('io.pixee.codefixer.java.JavaFixitCli');
        const outputFile = tempfile(".codetf");

        // TODO

        // the java module will have populated the output file
        return outputFile;
    }
}
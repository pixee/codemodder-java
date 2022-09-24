import {LanguageProvider} from "../providers";
import {CodeTLExecutionContext} from "../cli";

export class JavaLanguageProvider implements LanguageProvider {
    process(codetlExecutionContext: CodeTLExecutionContext): string {
        return "";
    }

}
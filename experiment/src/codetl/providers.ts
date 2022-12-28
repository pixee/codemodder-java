import {IncludesExcludes} from "./includes";
import {CodeTLRuleDefinition} from "../../../codetl-parser/src/codetl/parser";
import {LanguageProviderResult} from "./codetf";

/**
 * This type is responsible for providing support for a new language. They usually hand off to a backend process of some
 * kind in order to perform the actual CodeTF generation.
 */
export interface LanguageProvider {

    /**
     * Give the language provider backend all the execution context and return the CodeTF file path that it produces.
     */
    process(repositoryPath: string, includesExcludes: IncludesExcludes, codeTLRuleDefinitions: CodeTLRuleDefinition[]) : LanguageProviderResult;
}

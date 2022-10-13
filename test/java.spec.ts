import {fromConfiguration} from "../src/codetl/includes";

import {
    DefaultJavaCodeTLInterpreter,
    findJavaSourceDirectories,
    findJavaSourceFiles,
    JavaLanguageProvider
} from '../src/codetl/langs/java';
import {resolve} from "path";
import {CodeTLParser} from "../../codetl-parser/src/codetl/parser";
import {LanguageProviderResult} from "../src/codetl/codetf";
import {readFileSync} from "fs";

describe('Should get all source dirs and files', () => {
    const dirs = findJavaSourceDirectories("test/java_repo/")
    it("It finds the only src dir", () => {
        expect(dirs.length).toBe(1)
        expect(dirs[0]).toEqual("test/java_repo/src/main/java")
    })


    const files = findJavaSourceFiles(dirs)
    it("It finds the expected source files in src dir", () => {
        expect(files.length).toBe(3)
        expect(files).toContain(resolve("test/java_repo/src/main/java/acme_1.java"))
        expect(files).toContain(resolve("test/java_repo/src/main/java/acme_2.java"))
        expect(files).toContain(resolve("test/java_repo/src/main/java/acme/thing/thing.java"))
    })
});

function parseAndSetup(ruleStr : string) {
    const parser = new CodeTLParser();
    const parsedAst = parser.parse(ruleStr);
    if (parsedAst.errors.length != 0) {
        for (let error in parsedAst.errors) {
            console.log(error)
        }
        throw new Error("errors in compilation")
    }
    let rule = parser.convertAstToRuleDefinition(parsedAst.ast);
    let interpreter = new DefaultJavaCodeTLInterpreter();
    let lp = new JavaLanguageProvider(interpreter);
    let config = fromConfiguration("test/java_repo", [], [])
    return {rule, lp, config};
}

describe("It fails to process when undefined variable is referenced", () => {
    const badRuleStr: string = `
       rule pixee:java/bad-variable-definition
       match
          ConstructorCall $a {
             type = "java.util.Random"
          }
       replace $b
          ConstructorCall {
             type = "com.acme.AnotherType"
          }
       report "this shouldn't work because variable $b is never defined"
    `;

    const badInvocation = () => {
        let {rule, lp, config} = parseAndSetup(badRuleStr);
        lp.process(resolve("test/java_repo"), config, [rule]);
    }

    it("It throws an error  when executing", () => {
        expect(badInvocation).toThrowError()
    })
})

describe("It compiles and interprets", () => {
    const randomRuleStr : string = `
       rule pixee:java/secure-random
       match
          ConstructorCall $c {
             type = "java.util.Random"
          }
       replace $c 
          ConstructorCall {
             type = "java.security.SecureRandom"
          }
       report "upgraded prng"
    `;

    it("It creates the expected results from executing on the fake repo dir", () => {
        let {rule, lp, config} = parseAndSetup(randomRuleStr);
        let results : LanguageProviderResult = lp.process(resolve("test/java_repo"), config, [rule]);
        expect(results.vendor).toEqual("openpixee")
        expect(results.newDependencies).toHaveSize(0)
        expect(results.language).toEqual("java")
        expect(results.results).toHaveSize(1)
        expect(results.results[0].path).toEqual("/src/main/java/acme_1.java")
        expect(results.results[0].changes).toHaveSize(4)

        let expectedRandomFixLines : number[] = [6,9,12,16]
        let i =0
        for(let change of results.results[0].changes) {
            expect(change.category).toEqual("pixee:java/secure-random")
            expect(change.description).toEqual("upgraded prng")
            expect(change.properties.size).toBe(0)
            expect(change.lineNumber).toEqual(expectedRandomFixLines[i++])
        }

        expect(results.results[0].diff.trim()).toEqual(readFileSync("test/diffs/acme_1_after.diff").toString().trim())
    })
})
import {expect, test} from '@jest/globals';
import {fromConfiguration} from "../src/codetl/includes";

import {
    DefaultJavaCodeTLInterpreter,
    findJavaSourceDirectories,
    findJavaSourceFiles,
    JavaLanguageProvider
} from '../src/codetl/langs/java';
import {resolve} from "path";
import {CodeTLParser} from "../../codetl-parser/src/codetl/parser";

test('Should get all source dirs and files', () => {
    const dirs = findJavaSourceDirectories("test/java_repo/")
    expect(dirs.length).toBe(1)
    expect(dirs[0]).toEqual("test/java_repo/src/main/java")

    const files = findJavaSourceFiles(dirs)
    expect(files.length).toBe(3)
    expect(files).toContain(resolve("test/java_repo/src/main/java/acme_1.java"))
    expect(files).toContain(resolve("test/java_repo/src/main/java/acme_2.java"))
    expect(files).toContain(resolve("test/java_repo/src/main/java/acme/thing/thing.java"))
});

test("It compiles and interprets", () => {
    console.log(process.argv.join(" "))
    console.log(process.cwd())
    console.log(process.env)
    const ruleStr : string = `
       rule pixee:java/secure-random
       match
          ConstructorCall $c {
             type = "java.util.Random"
          }
       replace $c 
          ConstructorCall {
             type = "java.security.SecureRandom"
          }
    `;

    const parser = new CodeTLParser();
    const parsedAst = parser.parse(ruleStr);
    if(parsedAst.errors.length != 0) {
        for(let error in parsedAst.errors) {
            console.log(error)
        }
        throw new Error("errors in compilation")
    }
    let rule = parser.convertAstToRuleDefinition(parsedAst.ast);
    let interpreter = new DefaultJavaCodeTLInterpreter();
    let lp = new JavaLanguageProvider(interpreter);
    let config = fromConfiguration("test/java_repo", [],[])
    let results = lp.process(resolve("test/java_repo"), config, [rule]);
    console.log(JSON.stringify(results))
})
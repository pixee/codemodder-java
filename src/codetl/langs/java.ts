import {LanguageProvider} from "../providers";
import {Range} from "../location";
import tempfile from 'tempfile';
import {Node} from "../../../../codetl-parser/src/langdef/ast";
import {IncludesExcludes} from "../includes";
import { getAllFilesSync } from 'get-all-files'
import {CodeTLRuleDefinition, RequiredDependency} from "../../../../codetl-parser/src/codetl/parser";
import {readFileSync, readdirSync, writeFileSync, statSync} from "fs";
import path from "path";
import {createTwoFilesPatch} from 'diff'
import {
    CodeTFResult,
    CodeTFChange,
    LanguageProviderResult
} from "../codetf";

/**
 * This module supports analyzing/transforming Java code
 * @see https://github.com/openpixee/java-code-hardener
 */
export class JavaLanguageProvider implements LanguageProvider {
    private readonly interpreter: JavaCodeTLInterpreter;

    constructor(interpreter: JavaCodeTLInterpreter) {
        this.interpreter = interpreter;
    }

    process(repositoryPath: string, includesExcludes: IncludesExcludes, rules: CodeTLRuleDefinition[]): LanguageProviderResult {
        // get all java files in the repository and add them to a JavaParser context

        // @ts-ignore
        const javaParser = new (Java.type("com.github.javaparser.JavaParser"))
        // @ts-ignore
        const combinedTypeSolver = new (Java.type("com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver"));
        // @ts-ignore
        combinedTypeSolver.add(new (Java.type("com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver")));

        const javaSourceDirectories = findJavaSourceDirectories(repositoryPath)
        javaSourceDirectories.forEach(
            (javaDirectory) => {
                // @ts-ignore
                combinedTypeSolver.add(new (Java.type("com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver"))(javaDirectory))
            });

        // @ts-ignore
        javaParser
            .getParserConfiguration()
            // @ts-ignore
            .setSymbolResolver(new (Java.type("com.github.javaparser.symbolsolver.JavaSymbolSolver"))(combinedTypeSolver));

        const javaSourceFiles = findJavaSourceFiles(javaSourceDirectories)
        console.log("Processing " + javaSourceFiles.length + " files in repository")

        // @ts-ignore
        let lexicalPreservingPrinterType = Java.type("com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter");

        // for every java file, get the results of every CodeTL rule's effect on the file
        const newlyRequiredDependencies : RequiredDependency[] = []
        const erroredFiles : Array<string> = []
        const allResults : CodeTFResult[] = []
        for (const javaSourceFile of javaSourceFiles) {
            console.log("Processing " + javaSourceFile)
            // @ts-ignore
            const file = new (Java.type("java.io.File"))(javaSourceFile)

            // @ts-ignore
            const inputStream = new (Java.type("java.io.FileInputStream"))(file);
            const result = javaParser.parse(inputStream)

            // @ts-ignore
            if (!result.isSuccessful()) {
                erroredFiles.push(javaSourceFile)
                continue;
            }
            // @ts-ignore
            const compilationUnit = result.getResult().orElseThrow();

            // @ts-ignore
            lexicalPreservingPrinterType.setup(compilationUnit);

            let allRuleChanges : CodeTFChange[] = []
            for (const rule of rules) {
                const ruleChanges : CodeTFChange[] = this.interpreter.run(compilationUnit, rule);
                if(ruleChanges.length > 0) {
                    ruleChanges.forEach(change => allRuleChanges.push(change));
                    rule.requiredDependencies.forEach(dep => newlyRequiredDependencies.push(dep))
                }
            }
            if(allRuleChanges.length > 0) {
                // @ts-ignore
                const modifiedFileContents : string = lexicalPreservingPrinterType.print(compilationUnit)
                const modifiedFilePath = tempfile(".java")
                writeFileSync(modifiedFilePath, modifiedFileContents)
                const originalFileContents = readFileSync(javaSourceFile).toString()
                const relativePath : string = javaSourceFile.substring(repositoryPath.length)
                const diff : string = createTwoFilesPatch(relativePath, relativePath, originalFileContents, modifiedFileContents, null, null , {});
                allResults.push(new CodeTFResult(relativePath, diff, allRuleChanges))
            }
        }

        return new LanguageProviderResult("openpixee", "java", allResults, newlyRequiredDependencies)
    }
}

/**
 * Find all the "src/main/java" directories in a Java repository.
 */
export function findJavaSourceDirectories(dir : string) : Array<string> {
    let dirs : string[] = []
    readdirSync(dir).forEach( f => {
        let dirPath = path.join(dir, f);
        let isDirectory = statSync(dirPath).isDirectory();
        if(isDirectory) {
            if(dir.endsWith("/src/main/java")) {
                dirs.push(dir);
            }
            let recursiveDirectories = findJavaSourceDirectories(dirPath);
            recursiveDirectories.forEach(((recursiveDirectory) => {
                dirs.push(recursiveDirectory);
            }));
        }
    });
    return dirs;
}

/**
 * Find all the .java files in the source directories passed in.
 */
export function findJavaSourceFiles(javaSourceDirectories : string[]) : Array<string> {
    const javaSourceFiles : Array<string> = [];
    javaSourceDirectories.forEach((dir) => {
        const javaFiles = getAllFilesSync(dir, {resolve: true}).toArray()
            .filter(path => path.toLowerCase().endsWith("\.java"));
        javaFiles.forEach((file) => javaSourceFiles.push(file));
    });
    return javaSourceFiles;
}

/**
 * Responsible for running the Java CodeTL interpreter on a given JavaParser CompilationUnit type.
 */
export interface JavaCodeTLInterpreter {

    /**
     * This method will run the rule on the given CompilationUnit, transforming it as the rule sees fit, and the
     * changes are recorded in the return in the form of {@link CodeTFChange} elements.
     */
    run(compilationUnit: any, rule: CodeTLRuleDefinition) : CodeTFChange[]
}

export class DefaultJavaCodeTLInterpreter implements JavaCodeTLInterpreter {

    run(compilationUnit: any, rule: CodeTLRuleDefinition): CodeTFChange[] {
        // @ts-ignore
        const typeLocatorClass = Java.type("io.pixee.codefixer.java.TypeLocator")
        // @ts-ignore
        const typeLocator = typeLocatorClass.createDefault(compilationUnit);
        const matchNode = rule.matchNode;
        const matchAstNodeTypeName = matchNode.concept.name;
        const matchingAstNodes : JavaParserConstructor[] = []
        const changes : CodeTFChange[] = []
        const detectedImpliedImportsNeeded : string[] = []

        if(matchAstNodeTypeName === "ConstructorCall") {
            // @ts-ignore
            const constructors : JavaParserConstructor[] =
                // @ts-ignore
                Java.from(
                    // @ts-ignore
                    compilationUnit.findAll(Java.type("com.github.javaparser.ast.expr.ObjectCreationExpr"))
                ).map(con => new JavaParserConstructor(con, typeLocator))

            constructors.forEach(con => {
                if(this.constructorPropertiesMatch(matchNode, con)) {
                    matchingAstNodes.push(con);
                }
            });

            const replaceNode = rule.replaceNode
            if(replaceNode) {
                const replaceAstNodeTypeName = replaceNode.concept.name;
                const matchVariableName = replaceNode.nodeVariableName()
                const replaceVariableName = replaceNode.nodeVariableName()

                for(const matchingAstNode of matchingAstNodes) {
                    if(matchVariableName == replaceVariableName) {
                        // if the match and replace are both constructor calls, just overwrite the replaced elements
                        if(replaceAstNodeTypeName == "ConstructorCall") {
                            let typeFromReplace = replaceNode.valueFor("type") as string;
                            if(typeFromReplace != undefined) {
                                // save the FQCN for the import
                                detectedImpliedImportsNeeded.push(typeFromReplace)
                                // but for the code we're replacing, use the simple name
                                const simpleClassName = toSimpleClassName(typeFromReplace)
                                console.log("Replacing constructor type with: " + simpleClassName)
                                matchingAstNode.setType(typeFromReplace)
                                changes.push(new CodeTFChange(matchingAstNode.getRange().startLine, rule.ruleId.toIdentifier(),"", new Map<string,any>()))
                            }
                        }
                    }
                }
            }
        }

        if(changes.length > 0) {
            console.log("Since we changed code, we add the " + (rule.requiredImports.length + detectedImpliedImportsNeeded.length) + " imports needed")
            for(const impt of rule.requiredImports) {
                // @ts-ignore
                Java.type("io.pixee.codefixer.java.protections.ASTs").addImportIfMissing(compilationUnit, impt)
            }
            for(const impt of detectedImpliedImportsNeeded) {
                // @ts-ignore
                Java.type("io.pixee.codefixer.java.protections.ASTs").addImportIfMissing(compilationUnit, impt)
            }
        }

        return changes
    }

    /**
     * Compare a constructor node in our CodeTL format to the Java AST object.
     */
    private constructorPropertiesMatch(matchNode: Node, con : JavaParserConstructor) : boolean {
        let typeFromMatch = matchNode.valueFor("type") as string;
        if(typeFromMatch != undefined) {
            // we have to match on this
            const typeInCode : string = con.getResolvedType()
            console.log("Comparing " + typeFromMatch + " to " + typeInCode)
            if(typeInCode != typeFromMatch) {
                return false;
            }
        }
        let range = con.getRange()
        console.log("Matched at " + JSON.stringify(range))
        return true;
    }

}

/**
 * Represents an AST node that has a source code range.
 */
interface RangedNode {
    getRange() : Range
}

interface JavaConstructor {
    getTypeAsWritten() : string,
    getResolvedType() : string
    setType(newType : string)
}

/**
 * A proxy for JavaParser's ObjectCreationExpr object.
 */
class JavaParserConstructor implements JavaConstructor, RangedNode {
    private readonly range: Range;
    private readonly objectCreationExpr: any;
    private readonly typeLocator: any;

    constructor(objectCreationExpr : any, typeLocator : any) {
        this.objectCreationExpr = objectCreationExpr;
        this.typeLocator = typeLocator;
        this.range = toRange(objectCreationExpr.getRange().get())
    }

    getRange(): Range {
        return this.range;
    }

    getResolvedType(): string {
        // @ts-ignore
        return this.typeLocator.locateType(this.objectCreationExpr);
    }

    getTypeAsWritten(): string {
        // @ts-ignore
        return this.objectCreationExpr.getTypeAsString();
    }

    setType(newType: string) {
        this.objectCreationExpr.setType(newType)
    }
}

/**
 * Converts a JavaParser range to our common model.
 */
function toRange(param: any) : Range {
    return {
        startLine: param.begin.line,
        startColumn: param.begin.column,
        endLine: param.end.line,
        endColumn: param.end.column
    };
}

/**
 * Convert a Java type name to its "simple" version, i.e., without a package:
 *
 * java.util.Foo becomes Foo
 * Bar becomes Bar
 */
function toSimpleClassName(type: string) {
    return type.substring(type.lastIndexOf(".")+1)
}

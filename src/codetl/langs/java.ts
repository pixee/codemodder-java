import {LanguageProvider} from "../providers";
import {Range} from "../location";
import tempfile from 'tempfile';
import {Node} from "../../../../codetl-parser/src/langdef/ast";
import {IncludesExcludes} from "../includes";
import { getAllFilesSync } from 'get-all-files'
import {CodeTLRuleDefinition, ReplaceCommand, RequiredDependency} from "../../../../codetl-parser/src/codetl/parser";
import {readFileSync, readdirSync, writeFileSync, statSync} from "fs";
import path, {resolve} from "path";
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

        // @ts-ignore
        let lexicalPreservingPrinterType = Java.type("com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter");

        // for every java file, get the results of every CodeTL rule's effect on the file
        const newlyRequiredDependencies : RequiredDependency[] = []
        const erroredFiles : Array<string> = []
        const allResults : CodeTFResult[] = []
        for (const javaSourceFile of javaSourceFiles) {
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
                const relativePath : string = resolve(javaSourceFile).substring(resolve(repositoryPath).length)
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

/**
 * Describes a place in the AST that matched our "match" CodeTL section.
 */
class MatchContext {

    readonly rootMatchNode : JavaPaserASTNode
    readonly variables: Map<string, JavaPaserASTNode>

    constructor(rootMatchNode: JavaPaserASTNode, variables: Map<string, JavaPaserASTNode>) {
        this.rootMatchNode = rootMatchNode
        this.variables = variables
    }
}

export class DefaultJavaCodeTLInterpreter implements JavaCodeTLInterpreter {

    run(compilationUnit: any, rule: CodeTLRuleDefinition): CodeTFChange[] {
        // @ts-ignore
        const typeLocatorClass = Java.type("io.pixee.codefixer.java.TypeLocator")
        // @ts-ignore
        const typeLocator = typeLocatorClass.createDefault(compilationUnit);
        const rootMatchNode = rule.matchNode;
        const rootMatchAstNodeTypeName = rootMatchNode.concept.name;

        const changes : CodeTFChange[] = []
        const detectedImpliedImportsNeeded : Set<string> = new Set<string>()

        const matchContexts : MatchContext[] = []

        if(rootMatchAstNodeTypeName === "ConstructorCall") {
            // @ts-ignore
            const constructors : JavaParserConstructor[] =
                // @ts-ignore
                Java.from(
                    // @ts-ignore
                    compilationUnit.findAll(Java.type("com.github.javaparser.ast.expr.ObjectCreationExpr"))
                ).map(con => new JavaParserConstructor(con, typeLocator))

            constructors.forEach(con => {
                if(this.constructorPropertiesMatch(rootMatchNode, con)) {
                    const variableMap : Map<string,JavaPaserASTNode> = new Map<string,JavaPaserASTNode>()
                    if(rootMatchNode.nodeVariableName()) {
                        variableMap.set(rootMatchNode.nodeVariableName()!, con)
                    }
                    const matchContext : MatchContext = new MatchContext(con, variableMap)
                    matchContexts.push(matchContext)
                }
            });

            for(let replaceCommand of rule.replaceCommands) {
                const replaceNode : Node = replaceCommand.node
                const nodeTypeNameToReplaceWith = replaceNode.concept.name;

                for(const matchContext of matchContexts) {
                    if(!matchContext.variables.has(replaceCommand.variable)) {
                        throw new Error("no variable by that name to replace: " + replaceCommand)
                    }
                    const actualNodeToReplace : JavaPaserASTNode = matchContext.variables.get(replaceCommand.variable)!;
                    // if the match and replace are both constructor calls, just overwrite the replaced elements
                    if(nodeTypeNameToReplaceWith == "ConstructorCall" && actualNodeToReplace instanceof JavaParserConstructor) {
                        let typeFromReplace = replaceNode.valueFor("type") as string;
                        if(typeFromReplace != undefined) {
                            // save the FQCN for the import
                            detectedImpliedImportsNeeded.add(typeFromReplace)
                            // but for the code we're replacing, use the simple name
                            const simpleClassName = toSimpleClassName(typeFromReplace)
                            actualNodeToReplace.setType(simpleClassName)
                            changes.push(new CodeTFChange(actualNodeToReplace.getRange().startLine, rule.ruleId.toIdentifier(), rule.reportMessage ? rule.reportMessage : "", new Map<string,any>()))
                        }
                    }
                }
            }

        }

        if(changes.length > 0) {
            for(const impt of [...rule.requiredImports, ...detectedImpliedImportsNeeded]) {
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
            if(typeInCode != typeFromMatch) {
                return false;
            }
        }
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

interface JavaPaserASTNode {

}

/**
 * A proxy for JavaParser's ObjectCreationExpr object.
 */
class JavaParserConstructor implements JavaConstructor, RangedNode, JavaPaserASTNode {
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

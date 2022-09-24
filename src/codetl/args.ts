import yargs = require("yargs");

/**
 * Responsible for processing arguments from a command-line format into our objects, including sanitizing.
 */
export class ArgumentParser {

    /**
     * Return a parsed set of arguments.
     */
    parse(argString: string[]) {
        return yargs(argString).options({
            verbose: {
                type: 'boolean',
                default: false
            },
            repository: {
                type: 'string',
                describe: "repository path",
                demandOption: true
            },
            sarifFilePaths : {
              type: 'array',
              default: [],
              describe: 'path to a SARIF file to be incorporated as input (multiple)',
              demandOption: false
            },
            'rule-default' : {
                choices: ['enabled', 'disabled'],
                describe: 'Specify the default rule setting',
                default: 'enabled',
                demandOption: false
            },
            'rule-exception' : {
                default: [],
                describe: 'the rules that should have have the opposite of the default rule setting',
                demandOption: false
            },
            includes : {
                type: 'array',
                describe: 'the paths to include within the repository',
                demandOption: false
            },
            excludes : {
                type: 'array',
                describe: 'the paths to exclude within the repository',
                demandOption: false
            },
            output: {
                type: 'string',
                describe: "output file path",
                demandOption: true
            }
        }).parseSync();
    }
}
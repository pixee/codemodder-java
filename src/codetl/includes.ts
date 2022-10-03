import {resolve} from 'path';

/**
 * A type that's helpful for our traversal logic to understand if a given line is included in the configuration.
 */
export interface LineIncludesExcludes {
    /** Return true if the include/exclude rules allow changes to this line. */
    matches(line: number): boolean
}

export function fromIncludedLines(allowedLines: Set<number>) : LineIncludesExcludes {
    return new IncludeBasedLineIncludesExcludes(allowedLines);
}

export function fromExcludedLines(excludedLines: Set<number>) : LineIncludesExcludes {
    return new ExcludeBasedLineIncludesExcludes(excludedLines);
}

/** This is the main interaction point with types for detecting if a path should be included. */
export interface IncludesExcludes {

    /** Do we have any includes that include this file? */
    shouldInspect(file: string): boolean

    /** Do we have any includes that match the file and line number? */
    getIncludesExcludesForFile(file: string): LineIncludesExcludes;

}

class MatchesAllLines implements LineIncludesExcludes {
    matches(line: number): boolean {
        return true;
    }
}

/** Given a set of lines to include, determine if we should allow changes to this line. */
class IncludeBasedLineIncludesExcludes implements LineIncludesExcludes {

    private allowedNumbers: ReadonlySet<number>;
    constructor(numbers: ReadonlySet<number>) {
        this.allowedNumbers = numbers;
    }

    matches(line: number): boolean {
        return this.allowedNumbers.has(line);
    }

}

/** Given a set of lines to exclude, determine if we should allow changes to this line. */
class ExcludeBasedLineIncludesExcludes implements LineIncludesExcludes {

    private excludedNumbers: ReadonlySet<number>;

    constructor(numbers: ReadonlySet<number>) {
        this.excludedNumbers = numbers;
    }

    matches(line: number): boolean {
        return !this.excludedNumbers.has(line);
    }
}

class DefaultIncludesExcludes implements IncludesExcludes {

    private readonly pathIncludes : Array<PathMatcher>;
    private readonly pathExcludes : Array<PathMatcher>;

    constructor(pathIncludes: Array<PathMatcher>, pathExcludes: Array<PathMatcher>) {
        this.pathIncludes = pathIncludes;
        this.pathExcludes = pathExcludes;
    }

    shouldInspect(file : string) : boolean {
        if (this.pathIncludes.length != 0) {
            for (const pathInclude of this.pathIncludes) {
                if (pathInclude.matches(file)) {
                    // unless there is a deeper exclude, we match
                    for (const pathExclude of this.pathExcludes) {
                        if (pathExclude.matches(file) && pathExclude.hasLongerPathThan(pathInclude)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        for (const pathExclude of this.pathExcludes) {
            if (!pathExclude.targetsLine() && pathExclude.matches(file)) {
                return false;
            }
        }
        return true;
    }

    getIncludesExcludesForFile(file: string) : LineIncludesExcludes {
        const allowedLines = new Set<number>()
        for (const pathInclude of this.pathIncludes) {
            if (pathInclude.targetsLine()) {
                if (pathInclude.targetsFileExactly(file)) {
                    allowedLines.add(pathInclude.lineNumber());
                }
            }
        }

        const disallowedLines = new Set<number>()
        for (const pathExclude of this.pathExcludes) {
            if (pathExclude.targetsLine()) {
                if (pathExclude.targetsFileExactly(file)) {
                    disallowedLines.add(pathExclude.lineNumber());
                }
            }
        }

        if (allowedLines.size != 0 && disallowedLines.size != 0) {
            throw new Error("can't have both include and exclude targeting individual lines for a file");
        }

        if (allowedLines.size == 0 && disallowedLines.size == 0) {
            return new MatchesAllLines();
        }

        if (allowedLines.size != 0) {
            return fromIncludedLines(allowedLines);
        }
        return fromExcludedLines(disallowedLines);
    }
}

class MatchesEverything implements IncludesExcludes {
    shouldInspect(file : string) : boolean {
        return true;
    }
    getIncludesExcludesForFile(file: string): LineIncludesExcludes {
        return new MatchesAllLines();
    }
}


/**
 * Create an {@link IncludesExcludes} based on the configuration we're using -- the include
 * patterns and exclude patterns.
 */
export function fromConfiguration(
    repositoryRoot : string,
    includePatterns : Array<string>,
    excludePatterns : Array<string>) : IncludesExcludes{

    if (noPatternsSpecified(includePatterns, excludePatterns)) {
        return new MatchesEverything();
    }
    const pathIncludes : Array<PathMatcher> = [];
    const pathExcludes : Array<PathMatcher> = [];

    for (const includePattern of includePatterns) {
        pathIncludes.push(parsePattern(repositoryRoot, includePattern));
    }

    for (const excludePattern of excludePatterns) {
        pathExcludes.push(parsePattern(repositoryRoot, excludePattern));
    }

    return new DefaultIncludesExcludes(pathIncludes, pathExcludes);
}

function parsePattern(repositoryRoot : string, pattern : string) : PathMatcher {
    pattern = pattern.trim();

    // determine if this targets a line
    const lineSeparatorIndex : number = pattern.indexOf(':');

    let line : number
    let pathPrefix : string

    // if it targets a line, get the path and line separately
    if (lineSeparatorIndex != -1) {
        pathPrefix = pattern.substring(0, lineSeparatorIndex);
        line = parseInt(pattern.substring(lineSeparatorIndex + 1));
    } else {
        pathPrefix = pattern;
        line = -1;
    }
    return new PathMatcher(repositoryRoot, pathPrefix, line);
}

function noPatternsSpecified(
    includePatterns : Array<string>, excludePatterns : Array<string>) : boolean {
    return (includePatterns == null || includePatterns.length == 0)
        && (excludePatterns == null || excludePatterns.length == 0);
}

/** This type is used in include/exclude logic for matching paths. */
export class PathMatcher {

    private readonly pathPrefix : string;
    private readonly line : number;

    constructor(repositoryRoot : string, path : string, line : number) {
        this.pathPrefix = resolve(repositoryRoot + path)
        this.line = line;
    }

    /** Return if this path matcher matches the given file. */
    matches(file : string) : boolean {
        return resolve(file).startsWith(this.pathPrefix);
    }

    /**
     * Return true if this instance has a longer include/exclude than another, and thus overrules the
     * previous.
     */
    hasLongerPathThan(matcher : PathMatcher) : boolean {
        return this.pathPrefix.length > matcher.pathPrefix.length;
    }

    targetsFileExactly(file : string) : boolean {
        return resolve(file) === this.pathPrefix;
    }

    lineNumber() : number {
        return this.line;
    }

    targetsLine() : boolean {
        return this.line != -1;
    }
}
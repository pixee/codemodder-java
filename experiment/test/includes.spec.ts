import {fromConfiguration} from "../src/codetl/includes";
import {resolve} from "path";

describe('If no includes/excludes, everything allowed', () => {
    let includesExcludes = fromConfiguration("src/test/java_repo/", [], []);
    it("It inspects everything, including non-existent files", () => {
        expect(includesExcludes.shouldInspect("src/main/java/acme/acme_1.java")).toBe(true);
        expect(includesExcludes.shouldInspect("src/main/java/acme/acme_2.java")).toBe(true);
        expect(includesExcludes.shouldInspect("src/main/java/acme/thing/thing.java")).toBe(true);
        expect(includesExcludes.shouldInspect("non_existent.ts")).toBe(true);
        expect(includesExcludes.shouldInspect("src/test/non_existent.java")).toBe(true);
    })

});

describe('If only includes, should exclude everything else', () => {
    let includesExcludes = fromConfiguration("src/test/java_repo/", ["src/main/java"], []);
    const baseRepoPath = resolve("src/test/java_repo")
    it("It only inspects src/main/java but not other stuff", () => {
        expect(includesExcludes.shouldInspect(baseRepoPath + "//root.txt")).toBe(false);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/test/non_existent.java")).toBe(false);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/main/java/acme/acme_1.java")).toBe(true);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src///main///java/acme/acme_1.java")).toBe(true);
        expect(includesExcludes.shouldInspect(baseRepoPath + "//src///main///../main/java/acme/acme_1.java")).toBe(true);
    })
});

describe('If only excludes, should include everything else', () => {
    let includesExcludes = fromConfiguration("src/test/java_repo/", [], ["src/main/java/acme/thing"]);
    const baseRepoPath = resolve("src/test/java_repo")
    it("It only inspects stuff outside src/main/java/acme/thing", () => {
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/main/java/acme/thing/thing.java")).toBe(false);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/main/java/acme/thing/non_existent.java")).toBe(false);
        expect(includesExcludes.shouldInspect(baseRepoPath + "//root.txt")).toBe(true);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/test/non_existent.java")).toBe(true);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/main/java/acme/acme_1.java")).toBe(true);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src///main///java/acme/acme_1.java")).toBe(true);
        expect(includesExcludes.shouldInspect(baseRepoPath + "//src///main///../main/java/acme/acme_1.java")).toBe(true);
    })

});

describe('If including a single line in a file, ignore other lines', () => {
    let includesExcludes = fromConfiguration("src/test/java_repo/", ["src/main/java/acme/thing/thing.java:5"], []);
    const baseRepoPath = resolve("src/test/java_repo")
    it("It should only inspect the file for which the line number is attached to", () => {
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/test/non_existent.java")).toBe(false);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/main/java/acme/thing/thing.java")).toBe(true);
    })

    let includesExcludesForFile = includesExcludes.getIncludesExcludesForFile(baseRepoPath + "/src/main/java/acme/thing/thing.java");
    it("It only inspects line 5, the one from the configuration", () => {
        expect(includesExcludesForFile.matches(4)).toBe(false);
        expect(includesExcludesForFile.matches(5)).toBe(true);
        expect(includesExcludesForFile.matches(6)).toBe(false);
    })
});

describe('If excluding a single line in a file, include all files and other lines', () => {
    let includesExcludes = fromConfiguration("src/test/java_repo/", [], ["src/main/java/acme/thing/thing.java:5"]);
    const baseRepoPath = resolve("src/test/java_repo")
    it("When excluding a particular line number, all files should still be inspected", () => {
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/test/non_existent.java")).toBe(true);
        expect(includesExcludes.shouldInspect(baseRepoPath + "/src/main/java/acme/thing/thing.java")).toBe(true);
    })
    let includesExcludesForFile = includesExcludes.getIncludesExcludesForFile(baseRepoPath + "/src/main/java/acme/thing/thing.java");
    it("Only exclude line 5", () => {
        expect(includesExcludesForFile.matches(4)).toBe(true);
        expect(includesExcludesForFile.matches(5)).toBe(false);
        expect(includesExcludesForFile.matches(6)).toBe(true);
    })
});
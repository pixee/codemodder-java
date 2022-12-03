/**
 * Rule by rule, what do we think we can do with Contrast's Java results?
 *
 * <p>untrusted-deserialization: redundant to pixee but maybe could help with edge cases, like where
 * ObjectInputStream is fed back from a library or something? path-traversal: not sure how to safely
 * patch this one with high confidence of not breaking paths csrf-protection-disabled: removing this
 * protection will be often wrong as many pages are intended to have tokenless CSRF
 * cache-controls-missing: needs investigation because it's hard to imagine this will break
 * something, but could harm performance in rare situations crypto-bad-mac: changing this will break
 * stuff crypto-bad-cipher: changing this will break stuff crypto-weak-randomness: redundant to
 * pixee log-injection: this is probably not worth fixing except to void reports from testing tools
 * ssrf: redundant to pixee cmd-injection: redundant to pixee redos: maybe provide a
 * TimeBoxedPattern?? reflection-injection: we support this sql-injection: will need research but
 * can be done in some cases xpath-injection: similar to SQL injection trust-boundary-violation:
 * doesn't seem likely we could do anything here reflected/stored-xss: we support this for simple
 * Java and JSP cases
 */
package io.openpixee.java.plugins.contrast;

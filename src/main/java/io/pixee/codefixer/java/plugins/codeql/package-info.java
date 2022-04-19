/**
 * Rule by rule, what do we think we can do with Codeql's Java results?
 *
 * <p>cleartext-storage-in-cookie: command-line-injection: (netty-)http-response-splitting:
 * redundant to our protections insecure-cookie: we support this insecure-trustmanager: we should be
 * able to support this jhipster-prng: seems like this is redundant with our insecure randomness
 * protection jndi-injection: unsure ldap-injection: unsure maven/non-https-url: could support this
 * missing-jwt-signature-check: we support this
 * mvel-expression-injection/spel-expression-injection/ognl-injection/jexl-expression-injection: not
 * sure, could maybe sandbox? path-injection: not sure how to do this confidently, seems like
 * Contrast's path-traversal predictable-seed: could support this spring-disabled-csrf-protection:
 * would break things, this can't be done safely sql-injection: could be done ssrf: redundant
 * towards our protections stack-trace-exposure: we support this tainted-format-string: if we knew
 * the ranges of user controlled fmt string, we could escape that portion. seems nothing to be done
 * tainted-permissions-check: not sure anything to be done here -- what would you substitute it
 * with? seems extremely rare (unsafe-)deserialization: redundant to our protections
 * unsafe-hostname-verification: could replace with default, could be done
 * unvalidated-url-redirection: wouldn't know how to replace this weak-cryptographic-algorithm:
 * wouldn't know how to replace this world-writable-file-read: wouldn't know how to fix this
 * xml/xpath-injection: maybe like sql-injection this could be done xslt-injection: not sure how to
 * fix this, where else to pull an XSLT from?? seems extremely rare and not actionable xss: could be
 * done in many cases xxe: redundant towards our protections zipslip: redundant towards our
 * protections
 */
package io.pixee.codefixer.java.plugins.codeql;

package io.codemodder.remediation.sqlinjection;

/** Remediates HQL injection vulnerabilities. */
public interface HQLInjectionRemediator {

  HQLInjectionRemediator DEFAULT = new DefaultHQLInjectionRemediator();
}

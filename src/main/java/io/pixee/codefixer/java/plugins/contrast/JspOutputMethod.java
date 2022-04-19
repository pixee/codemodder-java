package io.pixee.codefixer.java.plugins.contrast;

import io.pixee.codefixer.java.JspLineWeave;

/** This type knows understands how JSP output method */
interface JspOutputMethod {

  /**
   * Return the number of times there may be writes of this type in the given line. Given that we're
   * not doing a true semantic analysis it is okay to be overly cautious and just return true if you
   * grep for tokens. There's little penalty if we're wrong and we over-report counts -- but
   * possibly bigger penalties if we under-report counts.
   */
  int countPossibleWrites(String line);

  /**
   * Given a JSP line of code with only one write of the type we represent, return it re-built with
   * an escaping mechanism built in.
   *
   * @param line the individual line to fix
   */
  JspLineWeave weaveLine(String line, String ruleId);
}

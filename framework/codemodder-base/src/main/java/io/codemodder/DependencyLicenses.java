package io.codemodder;

import java.util.List;

/** The license, if known, of a dependency. */
public final class DependencyLicenses {

  public static final String APACHE_2_0 = "Apache 2.0";
  public static final String BSD_3_CLAUSE = "BSD 3-Clause";
  public static final String BSD_2_CLAUSE = "BSD 2-Clause";
  public static final String MIT = "MIT";
  public static final String MPL_2_0 = "Mozilla Public License 2.0";
  public static final String EPL_1_0 = "Eclipse Public License 1.0";
  public static final String EPL_2_0 = "Eclipse Public License 2.0";
  public static final String LGPL_2_1 = "LGPL 2.1";
  public static final String LGPL_3_0 = "LGPL 3.0";
  public static final String GPL_2_0 = "GPL 2.0";
  public static final String GPL_3_0 = "GPL 3.0";
  public static final String AGPL_3_0 = "Affero GPL 3.0";
  public static final String UNLICENSE = "Unlicense";

  private static final List<String> openSourceLicenses =
      List.of(
          APACHE_2_0,
          BSD_3_CLAUSE,
          BSD_2_CLAUSE,
          MIT,
          MPL_2_0,
          EPL_1_0,
          EPL_2_0,
          LGPL_2_1,
          LGPL_3_0,
          GPL_2_0,
          GPL_3_0,
          AGPL_3_0,
          UNLICENSE);

  private DependencyLicenses() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static boolean isOpenSource(final String licenseName) {
    return openSourceLicenses.contains(licenseName);
  }
}

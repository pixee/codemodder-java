package io.openpixee.codemod;

import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.compiler.Environment;
import spoon.support.sniper.SniperJavaPrettyPrinter;

/** Static factory methods for creating OpenPixee SpoonAPI instances. */
public class SpoonAPIFactory {

  /**
   * Factory for creating a {@link SpoonAPI} configured with a pretty-printing strategy that
   * optimizes for code fidelity.
   *
   * @return new {@link SpoonAPI}
   */
  public static SpoonAPI create() {
    var spoon = new Launcher();
    final Environment environment = spoon.getEnvironment();
    environment.setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(environment));
    return spoon;
  }
}

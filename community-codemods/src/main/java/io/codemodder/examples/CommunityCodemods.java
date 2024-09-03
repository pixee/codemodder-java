package io.codemodder.examples;

import io.codemodder.Runner;
import java.util.List;

/** Runs the community codemods. */
public final class CommunityCodemods {

  /**
   * @param args the arguments to pass to the runner
   */
  public static void main(final String[] args) {
    Runner.run(List.of(MakeJUnit5TestsFinalCodemod.class, MakeJUnit5TestsFinalCodemod.class), args);
  }
}

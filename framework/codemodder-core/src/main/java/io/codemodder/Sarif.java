package io.codemodder;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.ThreadFlowLocation;
import java.util.List;

/** Utility class for working with SARIF model objects. */
public final class Sarif {

  private Sarif() {}

  /**
   * This method returns the last data flow location in the first code flow's first thread flow.
   * This assumes that the SARIF tool models their data flow in this way. This appears true for now.
   *
   * @param result the SARIF result
   * @return the last data flow location in the first code flow's first thread flow
   */
  public static Region getLastDataFlowRegion(final Result result) {
    List<ThreadFlowLocation> threadFlow =
        result.getCodeFlows().get(0).getThreadFlows().get(0).getLocations();
    PhysicalLocation lastLocation =
        threadFlow.get(threadFlow.size() - 1).getLocation().getPhysicalLocation();
    return lastLocation.getRegion();
  }
}

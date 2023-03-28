package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.ThreadFlowLocation;

/** A type responsible for extracting a region from a SARIF result */
public interface RegionExtractor {

  /** Extract a region from a SARIF result. */
  Region from(Result result);

  RegionExtractor FROM_FIRST_LOCATION =
      result -> result.getLocations().get(0).getPhysicalLocation().getRegion();

  RegionExtractor FROM_FIRST_THREADFLOW_EVENT =
      result -> {
        ThreadFlowLocation location =
            result.getCodeFlows().get(0).getThreadFlows().get(0).getLocations().get(0);
        return location.getLocation().getPhysicalLocation().getRegion();
      };
}

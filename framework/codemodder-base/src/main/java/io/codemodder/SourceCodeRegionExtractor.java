package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.ThreadFlowLocation;

/** A type responsible for extracting a region from a result */
public interface SourceCodeRegionExtractor<T> {

  /** Extract a region from a SARIF result. */
  SourceCodeRegion from(T t);

  SourceCodeRegionExtractor<Result> FROM_SARIF_FIRST_LOCATION =
      result -> {
        Region region = result.getLocations().get(0).getPhysicalLocation().getRegion();
        return SourceCodeRegion.fromSarif(region);
      };

  SourceCodeRegionExtractor<Result> FROM_SARIF_FIRST_THREADFLOW_EVENT =
      result -> {
        ThreadFlowLocation location =
            result.getCodeFlows().get(0).getThreadFlows().get(0).getLocations().get(0);

        Region region = location.getLocation().getPhysicalLocation().getRegion();
        return SourceCodeRegion.fromSarif(region);
      };
}

package io.codemodder;

import java.util.List;
import java.util.Set;

/** A type that provides {@link RuleSarif}s for consumption by {@link Codemod}s. */
public interface RuleSarifProvider {

  public Set<RuleSarif> getAllRuleSarifs();

  public Set<RuleSarif> getRuleSarifsByTool(String tool);

  public Set<RuleSarif> getRuleSarifsByRuleId(List<String> ruleId);
}

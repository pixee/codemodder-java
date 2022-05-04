package io.pixee.codefixer.java.protections;

/**
 * Targets making sure the protocols set in {@link
 * javax.net.ssl.SSLEngine#setEnabledProtocols(String[])} are safe.
 */
public final class SSLEngineSetEnabledProtocolsVisitorFactory extends SSLProtocolsVisitorFactory {

  public SSLEngineSetEnabledProtocolsVisitorFactory() {
    super("setEnabledProtocols", "SSLEngine", "javax.net.ssl.SSLEngine");
  }

  @Override
  public String ruleId() {
    return tlsVersionUpgradeRuleId;
  }

  private static final String tlsVersionUpgradeRuleId =
      "pixee:java/tls-version-upgrade-sslengine-setenabledprotocols";
}

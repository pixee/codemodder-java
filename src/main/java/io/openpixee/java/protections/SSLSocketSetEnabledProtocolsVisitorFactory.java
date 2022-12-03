package io.openpixee.java.protections;

/**
 * Targets making sure the protocols set in {@link
 * javax.net.ssl.SSLSocket#setEnabledProtocols(String[])} are safe.
 */
public final class SSLSocketSetEnabledProtocolsVisitorFactory extends SSLProtocolsVisitorFactory {

  public SSLSocketSetEnabledProtocolsVisitorFactory() {
    super("setEnabledProtocols", "SSLSocket", "javax.net.ssl.SSLSocket");
  }

  @Override
  public String ruleId() {
    return tlsVersionUpgradeRuleId;
  }

  private static final String tlsVersionUpgradeRuleId = "pixee:java/upgrade-sslsocket-tls";
}

package io.pixee.codefixer.java.protections;

/**
 * Targets making sure the protocols set in {@link
 * javax.net.ssl.SSLParameters#setProtocols(String[])} are safe.
 */
public final class SSLParametersSetProtocolsVisitorFactory extends SSLProtocolsVisitorFactory {

  public SSLParametersSetProtocolsVisitorFactory() {
    super("setProtocols", "SSLParameters", "javax.net.ssl.SSLParameters");
  }

  @Override
  public String ruleId() {
    return tlsVersionUpgradeRuleId;
  }

  private static final String tlsVersionUpgradeRuleId = "pixee:java/upgrade-sslparameters-tls";
}

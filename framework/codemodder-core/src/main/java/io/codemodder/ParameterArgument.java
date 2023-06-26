package io.codemodder;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/** Describes a parameter argument for a codemod. */
record ParameterArgument(String codemodId, String file, String line, String name, String value) {

  ParameterArgument {
    Objects.requireNonNull(codemodId);
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
  }

  /**
   * Converts an LDAP
   *
   * @param str
   * @return
   */
  static ParameterArgument fromNameValuePairs(final String str) {
    try {
      LdapName dn = new LdapName(str);
      Map<String, String> map =
          dn.getRdns().stream()
              .collect(Collectors.toMap(Rdn::getType, rdn -> (String) rdn.getValue()));
      return new ParameterArgument(
          map.get("codemod"), map.get("file"), map.get("line"), map.get("name"), map.get("value"));
    } catch (InvalidNameException e) {
      throw new IllegalArgumentException("Invalid name=value format for parameter argument", e);
    }
  }
}

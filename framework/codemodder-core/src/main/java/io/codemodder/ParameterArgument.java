package io.codemodder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/** Describes a parameter argument for a codemod. */
record ParameterArgument(String codemodId, String file, Integer line, String name, String value) {

  ParameterArgument {
    Objects.requireNonNull(codemodId);
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);
  }

  /** Converts an LDAP DN-formed RFC 4514 string into a {@link ParameterArgument}. */
  static ParameterArgument fromNameValuePairs(final String str) {
    try {
      LdapName dn = new LdapName(str);
      Map<String, String> map =
          dn.getRdns().stream()
              .collect(Collectors.toMap(Rdn::getType, rdn -> (String) rdn.getValue()));

      List<String> expectedKeys = List.of("codemod", "file", "line", "name", "value");
      if (map.keySet().stream().anyMatch(k -> !expectedKeys.contains(k))) {
        throw new IllegalArgumentException("Unexpected key for codemod parameter");
      }
      String codemod = map.get("codemod");
      String file = map.get("file");
      String line = map.get("line");
      String name = map.get("name");
      String value = map.get("value");
      return new ParameterArgument(
          codemod, file, line != null ? Integer.parseInt(line) : null, name, value);
    } catch (InvalidNameException e) {
      throw new IllegalArgumentException("Invalid name=value format for parameter argument", e);
    }
  }
}

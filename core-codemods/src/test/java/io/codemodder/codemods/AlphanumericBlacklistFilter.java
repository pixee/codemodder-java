package io.codemodder.codemods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class AlphanumericBlacklistFilterTest {

  String validateTableName(final String tablename) {
    Pattern regex = Pattern.compile("[a-zA-Z0-9_]+(.[a-zA-Z0-9_]+)?");
    if (!regex.matcher(tablename).matches()) {
      throw new SecurityException("Supplied table name contains non-alphanumeric characters");
    }
    return tablename;
  }

  @Test
  void it_tests_basic_alpha_string() {
    String tablename = "the_quick_brown_fox_jumps_over_the_lazy_dog_1234567890";
    assertEquals(validateTableName(tablename), tablename);
  }

  @Test
  void it_accepts_schema_table_name() {
    String tablename = "schema.table";
    assertEquals(validateTableName(tablename), tablename);
  }

  @Test
  void it_rejects_non_alphanumeric() {
    String tablename = "\"reject_this\" where 1=1";
    SecurityException e = assertThrows(SecurityException.class, () -> validateTableName(tablename));
    assertEquals("Supplied table name contains non-alphanumeric characters", e.getMessage());
  }
}

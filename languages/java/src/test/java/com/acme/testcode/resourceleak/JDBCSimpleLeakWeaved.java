package com.acme.testcode.resourceleak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCSimpleLeakWeaved {

  private Connection conn;

  public void method(String query) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      ResultSet rs = stmt.executeQuery(query);
    }
  }
}

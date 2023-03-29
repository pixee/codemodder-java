package com.acme.testcode.resourceleak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class JDBCAssignedToParameterLeak {

  private Connection conn;

  public void method(String query, ResultSet rs) throws SQLException {
    Statement stmt = conn.createStatement();
    rs = stmt.executeQuery(query);
  }
}

package com.acme.testcode.resourceleak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCNoFixRSAssignedOutOfScope {

  private Connection conn;

  public void method(String query) throws SQLException {
    ResultSet rs;
    {
      var stmt = conn.createStatement();
      rs = stmt.executeQuery(query);
    }
    rs.getRow();
  }
}

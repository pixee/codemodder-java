package com.acme.testcode.resourceleak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCNoFixReturningResultSet {

  private Connection conn;

  public ResultSet method(String query) throws SQLException {
    ResultSet rs;
    var stmt = conn.createStatement();
    rs = stmt.executeQuery(query);
    return rs;
  }
}

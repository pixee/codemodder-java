package com.acme.testcode.resourceleak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCNoFixRSAssignedToAField {

  private Connection conn;
  private ResultSet field;

  public void method(String query) throws SQLException {
    ResultSet rs;
    var stmt = conn.createStatement();
    rs = stmt.executeQuery(query);
    field = rs;
  }
}

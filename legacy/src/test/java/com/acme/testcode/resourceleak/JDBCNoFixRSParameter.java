package com.acme.testcode.resourceleak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JDBCNoFixRSParameter {

  private Connection conn;

  private ResultSet field;

  public void outside(ResultSet rs) {
    field = rs;
  }

  public void method4(String query) throws SQLException {
    ResultSet rs;
    var stmt = conn.createStatement();
    rs = stmt.executeQuery(query);
    outside(rs);
  }
}

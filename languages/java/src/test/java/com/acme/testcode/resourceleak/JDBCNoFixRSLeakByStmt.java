package com.acme.testcode.resourceleak;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JDBCNoFixRSLeakByStmt {

  private Connection conn;

  public Statement method(String query) throws SQLException {
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(query);
    return stmt;
  }

  public void method2(String query) throws SQLException {
    var stmt = method(query);
    var rs = stmt.getResultSet();
    rs.getRow();
  }
}

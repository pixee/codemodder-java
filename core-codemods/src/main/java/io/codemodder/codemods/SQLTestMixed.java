package io.codemodder.codemods;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public final class SQLTestMixed {

  private Connection conn;

  public ResultSet simpleIndirect() throws SQLException {
    Scanner scanner = new Scanner(System.in);
    String input = scanner.nextLine();
    String sql = "SELECT * FROM " + input + " where name=?";
    PreparedStatement stmt = conn.prepareStatement(sql);
    stmt.setString(1, scanner.nextLine());
    return stmt.execute();
  }
}

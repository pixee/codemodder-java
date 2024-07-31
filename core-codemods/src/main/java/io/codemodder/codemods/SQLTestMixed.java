package io.codemodder.codemods;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
public final class SQLTestMixed {

    private Connection conn;

    public ResultSet simpleIndirect() throws SQLException {
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine();
        String input2 = scanner.nextLine();
        String sql = "SELECT * FROM " + input + " where name='" + input2 + "'" ;
        Statement stmt = conn.createStatement();
        return stmt.executeQuery(sql);
    }

}

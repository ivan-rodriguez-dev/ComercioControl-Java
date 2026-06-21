package app.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionSQLite {

    private static final String URL = "jdbc:sqlite:comerciocontrol.db";

    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
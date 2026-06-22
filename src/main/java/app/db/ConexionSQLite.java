package app.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionSQLite {

    public static Connection conectar() throws SQLException {
        Connection conn = DriverManager.getConnection(app.util.Rutas.dbUrl());
        // Habilitar foreign keys en cada conexión
        try (var stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}

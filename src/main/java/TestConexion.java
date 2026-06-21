import java.sql.Connection;

import java.sql.DriverManager;



public class TestConexion {

    public static void main(String[] args) throws Exception {

        String url = "jdbc:sqlite:comerciocontrol.db";

        try (Connection con = DriverManager.getConnection(url)) {

            System.out.println("[OK] Conexion a la base de datos EXITOSA.");

        }

    }

} 
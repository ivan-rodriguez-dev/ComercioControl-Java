package app.dao;

import app.db.ConexionSQLite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acceso a la tabla clave-valor de configuración del sistema. */
public class ConfiguracionDAO {

    public String get(String clave, String porDefecto) {
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String v = rs.getString(1);
                return v != null ? v : porDefecto;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return porDefecto;
    }

    public void set(String clave, String valor) {
        String sql = "INSERT INTO configuracion(clave, valor) VALUES(?, ?) " +
                     "ON CONFLICT(clave) DO UPDATE SET valor = excluded.valor";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setString(2, valor);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

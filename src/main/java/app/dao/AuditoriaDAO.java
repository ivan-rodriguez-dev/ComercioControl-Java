package app.dao;

import app.db.ConexionSQLite;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditoriaDAO {

    public void registrar(int usuarioId, String accion, String tabla, String detalle) {
        String sql = "INSERT INTO auditoria (usuario_id, accion, tabla_affected, detalle) VALUES (?,?,?,?)";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, accion);
            ps.setString(3, tabla);
            ps.setString(4, detalle);
            ps.executeUpdate();
        } catch (SQLException e) { /* auditoria no es crítica */ }
    }

    public List<String[]> listar(int limite) {
        List<String[]> lista = new ArrayList<>();
        String sql = """
            SELECT a.fecha, u.nombre as usuario, a.accion, a.tabla_affected, a.detalle
            FROM auditoria a
            JOIN usuarios u ON u.id = a.usuario_id
            ORDER BY a.id DESC LIMIT ?
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(new String[]{
                    rs.getString("fecha"),
                    rs.getString("usuario"),
                    rs.getString("accion"),
                    rs.getString("tabla_affected"),
                    rs.getString("detalle")
                });
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }
}

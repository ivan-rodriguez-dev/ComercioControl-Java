package app.dao;

import app.db.ConexionSQLite;
import app.model.Caja;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CajaDAO {

    public Caja obtenerCajaHoy() {
        String sql = """
            SELECT c.*, u.nombre as usuario_nombre
            FROM caja c JOIN usuarios u ON u.id = c.usuario_id
            WHERE c.fecha = date('now','localtime') AND c.estado = 'abierta'
            ORDER BY c.id DESC LIMIT 1
        """;
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    public BigDecimal totalVentasHoy() {
        String sql = "SELECT COALESCE(SUM(total),0) FROM ventas WHERE date(fecha)=date('now','localtime')";
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return BigDecimal.valueOf(rs.getDouble(1));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return BigDecimal.ZERO;
    }

    public int contarVentasHoy() {
        String sql = "SELECT COUNT(*) FROM ventas WHERE date(fecha)=date('now','localtime')";
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
        return 0;
    }

    public void abrirCaja(BigDecimal montoApertura, int usuarioId) {
        String sql = """
            INSERT INTO caja (fecha, apertura, cierre, ventas_dia, diferencia, usuario_id, estado)
            VALUES (date('now','localtime'), ?, 0, 0, 0, ?, 'abierta')
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, montoApertura);
            ps.setInt(2, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public BigDecimal obtenerApertura(int cajaId) {
        String sql = "SELECT apertura FROM caja WHERE id = ?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, cajaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return BigDecimal.valueOf(rs.getDouble("apertura"));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return BigDecimal.ZERO;
    }

    public void cerrarCaja(int cajaId, BigDecimal montoCierre, String observacion) {
        BigDecimal ventasDia = totalVentasHoy();
        // Efectivo esperado = apertura + ventas del día. Diferencia = contado - esperado.
        BigDecimal esperado = obtenerApertura(cajaId).add(ventasDia);
        BigDecimal diferencia = montoCierre.subtract(esperado);
        String sql = "UPDATE caja SET cierre=?, ventas_dia=?, diferencia=?, observacion=?, estado='cerrada' WHERE id=?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, montoCierre);
            ps.setBigDecimal(2, ventasDia);
            ps.setBigDecimal(3, diferencia);
            ps.setString(4, observacion);
            ps.setInt(5, cajaId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Caja> listarHistorial(int limite) {
        List<Caja> lista = new ArrayList<>();
        String sql = """
            SELECT c.*, u.nombre as usuario_nombre
            FROM caja c JOIN usuarios u ON u.id = c.usuario_id
            ORDER BY c.id DESC LIMIT ?
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }

    private Caja mapear(ResultSet rs) throws SQLException {
        Caja c = new Caja();
        c.setId(rs.getInt("id"));
        c.setFecha(rs.getString("fecha"));
        c.setApertura(BigDecimal.valueOf(rs.getDouble("apertura")));
        c.setCierre(BigDecimal.valueOf(rs.getDouble("cierre")));
        c.setVentasDia(BigDecimal.valueOf(rs.getDouble("ventas_dia")));
        c.setDiferencia(BigDecimal.valueOf(rs.getDouble("diferencia")));
        c.setObservacion(rs.getString("observacion"));
        c.setUsuarioId(rs.getInt("usuario_id"));
        c.setEstado(rs.getString("estado"));
        try { c.setUsuarioNombre(rs.getString("usuario_nombre")); } catch (SQLException ignored) {}
        return c;
    }
}

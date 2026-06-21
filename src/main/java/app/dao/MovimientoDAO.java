package app.dao;

import app.db.ConexionSQLite;
import app.model.Movimiento;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void registrar(Movimiento m) {
        String sqlMov = """
            INSERT INTO movimientos (producto_id, tipo, cantidad, stock_resultante, fecha, usuario_id, observacion)
            VALUES (?,?,?,?,?,?,?)
        """;
        String sqlStock = "UPDATE productos SET stock_actual = ? WHERE id = ?";

        try (Connection conn = ConexionSQLite.conectar()) {
            conn.setAutoCommit(false);
            try {
                PreparedStatement psMov = conn.prepareStatement(sqlMov, Statement.RETURN_GENERATED_KEYS);
                psMov.setInt(1, m.getProductoId());
                psMov.setString(2, m.getTipo().name());
                psMov.setInt(3, m.getCantidad());
                psMov.setInt(4, m.getStockResultante());
                psMov.setString(5, LocalDateTime.now().format(FMT));
                psMov.setInt(6, m.getUsuarioId());
                psMov.setString(7, m.getObservacion());
                psMov.executeUpdate();
                ResultSet keys = psMov.getGeneratedKeys();
                if (keys.next()) m.setId(keys.getInt(1));

                PreparedStatement psStock = conn.prepareStatement(sqlStock);
                psStock.setInt(1, m.getStockResultante());
                psStock.setInt(2, m.getProductoId());
                psStock.executeUpdate();

                conn.commit();
                new AuditoriaDAO().registrar(m.getUsuarioId(),
                        m.getTipo().name().toUpperCase(), "movimientos",
                        m.getProductoNombre() + " · " + m.getCantidad() + " uds → stock " + m.getStockResultante());
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Movimiento> listarTodos() {
        return listarFiltrado(null);
    }

    public List<Movimiento> listarFiltrado(String tipo) {
        List<Movimiento> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT m.*, p.nombre as producto_nombre, u.nombre as usuario_nombre
            FROM movimientos m
            JOIN productos p ON p.id = m.producto_id
            JOIN usuarios u ON u.id = m.usuario_id
            WHERE 1=1
        """);
        if (tipo != null && !tipo.isBlank() && !tipo.equals("Todos los tipos")) {
            sql.append(" AND m.tipo = '").append(tipo.toLowerCase()).append("'");
        }
        sql.append(" ORDER BY m.fecha DESC LIMIT 200");

        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return lista;
    }

    private Movimiento mapear(ResultSet rs) throws SQLException {
        Movimiento m = new Movimiento();
        m.setId(rs.getInt("id"));
        m.setProductoId(rs.getInt("producto_id"));
        m.setProductoNombre(rs.getString("producto_nombre"));
        m.setTipo(Movimiento.Tipo.valueOf(rs.getString("tipo")));
        m.setCantidad(rs.getInt("cantidad"));
        m.setStockResultante(rs.getInt("stock_resultante"));
        String fechaStr = rs.getString("fecha");
        if (fechaStr != null) m.setFecha(LocalDateTime.parse(fechaStr, FMT));
        m.setUsuarioId(rs.getInt("usuario_id"));
        m.setUsuarioNombre(rs.getString("usuario_nombre"));
        m.setObservacion(rs.getString("observacion"));
        return m;
    }
}

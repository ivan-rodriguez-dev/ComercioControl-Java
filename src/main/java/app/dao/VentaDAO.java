package app.dao;

import app.db.ConexionSQLite;
import app.model.DetalleVenta;
import app.model.Venta;
import app.util.SessionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public int registrarVenta(Venta venta) {
        String sqlVenta = "INSERT INTO ventas (fecha, total, descuento, iva, usuario_id, cliente_id) VALUES (?,?,?,?,?,?)";
        String sqlDetalle = "INSERT INTO detalle_ventas (venta_id, producto_id, cantidad, precio_unitario) VALUES (?,?,?,?)";
        String sqlStock = "UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ?";

        try (Connection conn = ConexionSQLite.conectar()) {
            conn.setAutoCommit(false);
            try {
                // Insertar venta
                PreparedStatement psVenta = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS);
                psVenta.setString(1, LocalDateTime.now().format(FMT));
                psVenta.setBigDecimal(2, venta.getTotal());
                psVenta.setBigDecimal(3, venta.getDescuento());
                psVenta.setBigDecimal(4, venta.getIva());
                psVenta.setInt(5, venta.getUsuarioId());
                if (venta.getClienteId() > 0) psVenta.setInt(6, venta.getClienteId());
                else psVenta.setNull(6, java.sql.Types.INTEGER);
                psVenta.executeUpdate();
                ResultSet keys = psVenta.getGeneratedKeys();
                int ventaId = keys.next() ? keys.getInt(1) : -1;
                venta.setId(ventaId);

                // Insertar detalles y descontar stock
                PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle);
                PreparedStatement psStock = conn.prepareStatement(sqlStock);
                for (DetalleVenta d : venta.getDetalles()) {
                    psDetalle.setInt(1, ventaId);
                    psDetalle.setInt(2, d.getProductoId());
                    psDetalle.setInt(3, d.getCantidad());
                    psDetalle.setBigDecimal(4, d.getPrecioUnitario());
                    psDetalle.addBatch();

                    psStock.setInt(1, d.getCantidad());
                    psStock.setInt(2, d.getProductoId());
                    psStock.addBatch();
                }
                psDetalle.executeBatch();
                psStock.executeBatch();

                conn.commit();
                new AuditoriaDAO().registrar(venta.getUsuarioId(), "VENTA",
                        "ventas", "Venta #" + ventaId + " por $" + venta.getTotal());
                return ventaId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BigDecimal totalVentasHoy() {
        String sql = "SELECT COALESCE(SUM(total),0) FROM ventas WHERE date(fecha) = date('now','localtime')";
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return BigDecimal.valueOf(rs.getDouble(1));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal totalVentasMes() {
        String sql = "SELECT COALESCE(SUM(total),0) FROM ventas WHERE strftime('%Y-%m', fecha) = strftime('%Y-%m', 'now','localtime')";
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return BigDecimal.valueOf(rs.getDouble(1));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return BigDecimal.ZERO;
    }

    // Ventas por día de la semana actual (0=lunes..6=domingo)
    public double[] ventasPorDiaSemana() {
        double[] dias = new double[7];
        String sql = """
            SELECT strftime('%w', fecha) as dia, COALESCE(SUM(total),0) as total
            FROM ventas
            WHERE fecha >= date('now','localtime','-6 days')
            GROUP BY dia
        """;
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int dia = rs.getInt("dia"); // 0=domingo, 1=lunes...
                int idx = dia == 0 ? 6 : dia - 1; // convertir a lunes=0
                dias[idx] = rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return dias;
    }

    public java.util.LinkedHashMap<String, Double> ventasPorDiaPeriodo(String desde, String hasta) {
        java.util.LinkedHashMap<String, Double> map = new java.util.LinkedHashMap<>();
        String sql = """
            SELECT date(fecha) as dia, COALESCE(SUM(total),0) as total
            FROM ventas
            WHERE date(fecha) BETWEEN ? AND ?
            GROUP BY dia ORDER BY dia
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, desde); ps.setString(2, hasta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String d = rs.getString("dia");
                map.put(d.length() >= 10 ? d.substring(5) : d, rs.getDouble("total"));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return map;
    }

    public List<Object[]> topProductosVendidos(String desde, String hasta, int limit) {
        List<Object[]> lista = new ArrayList<>();
        String sql = """
            SELECT p.nombre,
                   COALESCE(SUM(dv.cantidad),0) as total_und,
                   COALESCE(SUM(dv.cantidad * dv.precio_unitario),0) as total_ing
            FROM detalle_ventas dv
            JOIN ventas v ON v.id = dv.venta_id
            JOIN productos p ON p.id = dv.producto_id
            WHERE date(v.fecha) BETWEEN ? AND ?
            GROUP BY p.id ORDER BY total_und DESC LIMIT ?
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, desde); ps.setString(2, hasta); ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(new Object[]{ rs.getString("nombre"), rs.getInt("total_und"), rs.getDouble("total_ing") });
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }

    public List<Venta> listarPorPeriodo(String desde, String hasta) {
        List<Venta> lista = new ArrayList<>();
        String sql = """
            SELECT v.*, u.nombre as usuario_nombre, c.nombre as cliente_nombre,
                   COALESCE((SELECT SUM(dv.cantidad) FROM detalle_ventas dv WHERE dv.venta_id = v.id),0) as total_unidades
            FROM ventas v
            JOIN usuarios u ON u.id = v.usuario_id
            LEFT JOIN clientes c ON c.id = v.cliente_id
            WHERE date(v.fecha) BETWEEN ? AND ?
            ORDER BY v.fecha DESC
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, desde);
            ps.setString(2, hasta);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return lista;
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getInt("id"));
        String fechaStr = rs.getString("fecha");
        if (fechaStr != null) v.setFecha(LocalDateTime.parse(fechaStr, FMT));
        v.setTotal(BigDecimal.valueOf(rs.getDouble("total")));
        v.setDescuento(BigDecimal.valueOf(rs.getDouble("descuento")));
        v.setIva(BigDecimal.valueOf(rs.getDouble("iva")));
        v.setUsuarioId(rs.getInt("usuario_id"));
        try { v.setUsuarioNombre(rs.getString("usuario_nombre")); } catch (SQLException ignored) {}
        try { v.setTotalUnidades(rs.getInt("total_unidades")); } catch (SQLException ignored) {}
        try { v.setClienteId(rs.getInt("cliente_id")); } catch (SQLException ignored) {}
        try { v.setClienteNombre(rs.getString("cliente_nombre")); } catch (SQLException ignored) {}
        return v;
    }
}

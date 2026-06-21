package app.dao;

import app.db.ConexionSQLite;
import app.model.Producto;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public List<Producto> listarTodos() {
        return buscar(null, null);
    }

    public List<Producto> buscar(String texto, String categoria) {
        List<Producto> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM productos WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            sql.append(" AND (nombre LIKE ? OR codigo LIKE ? OR categoria LIKE ?)");
            String like = "%" + texto.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (categoria != null && !categoria.isBlank() && !categoria.equals("Todas las categorías")) {
            sql.append(" AND categoria = ?");
            params.add(categoria);
        }
        sql.append(" ORDER BY codigo");

        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return lista;
    }

    public List<String> listarCategorias() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT DISTINCT categoria FROM productos WHERE categoria IS NOT NULL ORDER BY categoria";
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) cats.add(rs.getString(1));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return cats;
    }

    public Producto buscarPorId(int id) {
        String sql = "SELECT * FROM productos WHERE id = ?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void guardar(Producto p) {
        String sql = """
            INSERT INTO productos (codigo, nombre, categoria, precio_costo, precio_venta,
                stock_actual, stock_minimo, proveedor_id)
            VALUES (?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getCategoria());
            ps.setBigDecimal(4, p.getPrecioCosto());
            ps.setBigDecimal(5, p.getPrecioVenta());
            ps.setInt(6, p.getStockActual());
            ps.setInt(7, p.getStockMinimo());
            if (p.getProveedorId() != null) ps.setInt(8, p.getProveedorId());
            else ps.setNull(8, Types.INTEGER);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void actualizar(Producto p) {
        String sql = """
            UPDATE productos SET codigo=?, nombre=?, categoria=?, precio_costo=?, precio_venta=?,
                stock_actual=?, stock_minimo=?, proveedor_id=?
            WHERE id=?
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getCategoria());
            ps.setBigDecimal(4, p.getPrecioCosto());
            ps.setBigDecimal(5, p.getPrecioVenta());
            ps.setInt(6, p.getStockActual());
            ps.setInt(7, p.getStockMinimo());
            if (p.getProveedorId() != null) ps.setInt(8, p.getProveedorId());
            else ps.setNull(8, Types.INTEGER);
            ps.setInt(9, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void actualizarStock(int productoId, int nuevoStock) {
        String sql = "UPDATE productos SET stock_actual = ? WHERE id = ?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setInt(2, productoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM productos WHERE id = ?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int contarCriticos() {
        String sql = "SELECT COUNT(*) FROM productos WHERE stock_actual <= stock_minimo";
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public int contarActivos() {
        String sql = "SELECT COUNT(*) FROM productos";
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    /** Retorna [totalProductos, totalUnidades, valorCosto, valorVenta] */
    public double[] resumenInventario() {
        String sql = """
            SELECT COUNT(*),
                   COALESCE(SUM(stock_actual), 0),
                   COALESCE(SUM(stock_actual * precio_costo), 0),
                   COALESCE(SUM(stock_actual * precio_venta), 0)
            FROM productos
            """;
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return new double[]{rs.getDouble(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4)};
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new double[]{0, 0, 0, 0};
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setCodigo(rs.getString("codigo"));
        p.setNombre(rs.getString("nombre"));
        p.setCategoria(rs.getString("categoria"));
        p.setPrecioCosto(BigDecimal.valueOf(rs.getDouble("precio_costo")));
        p.setPrecioVenta(BigDecimal.valueOf(rs.getDouble("precio_venta")));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        int pid = rs.getInt("proveedor_id");
        p.setProveedorId(rs.wasNull() ? null : pid);
        return p;
    }
}

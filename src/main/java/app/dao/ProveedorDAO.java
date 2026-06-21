package app.dao;

import app.db.ConexionSQLite;
import app.model.Proveedor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProveedorDAO {

    public List<Proveedor> listarTodos() {
        List<Proveedor> lista = new ArrayList<>();
        String sql = """
            SELECT p.*, COUNT(pr.id) as cant_productos
            FROM proveedores p
            LEFT JOIN productos pr ON pr.proveedor_id = p.id
            GROUP BY p.id ORDER BY p.razon_social
        """;
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return lista;
    }

    public List<Proveedor> buscar(String texto) {
        List<Proveedor> lista = new ArrayList<>();
        String sql = """
            SELECT p.*, COUNT(pr.id) as cant_productos
            FROM proveedores p
            LEFT JOIN productos pr ON pr.proveedor_id = p.id
            WHERE p.razon_social LIKE ? OR p.nit LIKE ?
            GROUP BY p.id ORDER BY p.razon_social
        """;
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + texto + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return lista;
    }

    public void guardar(Proveedor p) {
        String sql = "INSERT INTO proveedores (nit, razon_social, contacto, telefono, email, activo) VALUES (?,?,?,?,?,?)";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getNit());
            ps.setString(2, p.getRazonSocial());
            ps.setString(3, p.getContacto());
            ps.setString(4, p.getTelefono());
            ps.setString(5, p.getEmail());
            ps.setInt(6, p.isActivo() ? 1 : 0);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) p.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void actualizar(Proveedor p) {
        String sql = "UPDATE proveedores SET nit=?, razon_social=?, contacto=?, telefono=?, email=?, activo=? WHERE id=?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getNit());
            ps.setString(2, p.getRazonSocial());
            ps.setString(3, p.getContacto());
            ps.setString(4, p.getTelefono());
            ps.setString(5, p.getEmail());
            ps.setInt(6, p.isActivo() ? 1 : 0);
            ps.setInt(7, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void eliminar(int id) {
        String sql = "DELETE FROM proveedores WHERE id = ?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setId(rs.getInt("id"));
        p.setNit(rs.getString("nit"));
        p.setRazonSocial(rs.getString("razon_social"));
        p.setContacto(rs.getString("contacto"));
        p.setTelefono(rs.getString("telefono"));
        p.setEmail(rs.getString("email"));
        p.setActivo(rs.getInt("activo") == 1);
        try { p.setCantidadProductos(rs.getInt("cant_productos")); } catch (SQLException ignored) {}
        return p;
    }
}

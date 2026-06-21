package app.dao;

import app.db.ConexionSQLite;
import app.model.Cliente;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public List<Cliente> listarTodos() {
        List<Cliente> lista = new ArrayList<>();
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM clientes ORDER BY nombre")) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }

    public List<Cliente> listarActivos() {
        List<Cliente> lista = new ArrayList<>();
        try (Connection conn = ConexionSQLite.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM clientes WHERE activo=1 ORDER BY nombre")) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }

    public List<Cliente> buscar(String texto) {
        if (texto == null || texto.isBlank()) return listarTodos();
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE nombre LIKE ? OR cedula LIKE ? ORDER BY nombre";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String q = "%" + texto + "%";
            ps.setString(1, q); ps.setString(2, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return lista;
    }

    public void guardar(Cliente c) {
        String sql = "INSERT INTO clientes (nombre, cedula, telefono, email, direccion, activo) VALUES (?,?,?,?,?,?)";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, nvl(c.getCedula()));
            ps.setString(3, nvl(c.getTelefono()));
            ps.setString(4, nvl(c.getEmail()));
            ps.setString(5, nvl(c.getDireccion()));
            ps.setInt(6, c.isActivo() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void actualizar(Cliente c) {
        String sql = "UPDATE clientes SET nombre=?, cedula=?, telefono=?, email=?, direccion=?, activo=? WHERE id=?";
        try (Connection conn = ConexionSQLite.conectar();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, nvl(c.getCedula()));
            ps.setString(3, nvl(c.getTelefono()));
            ps.setString(4, nvl(c.getEmail()));
            ps.setString(5, nvl(c.getDireccion()));
            ps.setInt(6, c.isActivo() ? 1 : 0);
            ps.setInt(7, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setCedula(rs.getString("cedula"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setDireccion(rs.getString("direccion"));
        c.setActivo(rs.getInt("activo") == 1);
        return c;
    }

    private String nvl(String s) { return s != null ? s : ""; }
}

package app.pruebas;

import app.dao.ClienteDAO;
import app.model.Cliente;
import app.model.Producto;
import app.model.Proveedor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RF-04 / RF-05 / RF-06 – Inventario, clientes y proveedores")
class InventarioProveedoresClientesPruebasTest extends BaseDatosPrueba {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    @Test @DisplayName("CP-014 Crear producto con datos válidos")
    void cp014() {
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 50, 10, null);
        Producto leido = productoDAO.buscarPorId(p.getId());
        assertEquals("Arroz 500 g", leido.getNombre());
        assertEquals(50, leido.getStockActual());
        assertEquals(0, new BigDecimal("3500").compareTo(leido.getPrecioVenta()));
    }

    @Test @DisplayName("CP-015 Producto sin nombre es rechazado por la BD (NOT NULL)")
    void cp015() {
        assertThrows(RuntimeException.class, () -> crearProducto("P002", null, "1000", 1, 1, null));
        assertEquals(0, productoDAO.contarActivos());
    }

    @Test @DisplayName("CP-016 Crear producto con código duplicado")
    void cp016() {
        crearProducto("P001", "Arroz 500 g", "3500", 50, 10, null);
        assertThrows(RuntimeException.class, () -> crearProducto("P001", "Otro", "1000", 1, 1, null));
        assertEquals(1, productoDAO.contarActivos());
    }

    @Test @DisplayName("CP-017 Alerta de stock crítico (stock 8 ≤ mínimo 10)")
    void cp017() {
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 50, 10, null);
        assertEquals(0, productoDAO.contarCriticos());
        p.setStockActual(8);
        productoDAO.actualizar(p);
        assertEquals(1, productoDAO.contarCriticos());
    }

    @Test @DisplayName("CP-018 Editar y eliminar un producto sin historial")
    void cp018() {
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 50, 10, null);
        p.setPrecioVenta(new BigDecimal("3800"));
        productoDAO.actualizar(p);
        assertEquals(0, new BigDecimal("3800").compareTo(productoDAO.buscarPorId(p.getId()).getPrecioVenta()));
        productoDAO.eliminar(p.getId());
        assertNull(productoDAO.buscarPorId(p.getId()));
    }

    @Test @DisplayName("CP-019 Crear cliente con datos válidos")
    void cp019() {
        Cliente c = new Cliente();
        c.setNombre("Carlos Pérez");
        c.setCedula("1012345678");
        c.setActivo(true);
        clienteDAO.guardar(c);
        assertTrue(clienteDAO.listarTodos().stream().anyMatch(x -> "1012345678".equals(x.getCedula())));
    }

    @Test @DisplayName("CP-021 Crear cliente con cédula duplicada")
    void cp021() {
        Cliente c = new Cliente();
        c.setNombre("Carlos Pérez"); c.setCedula("1012345678"); c.setActivo(true);
        clienteDAO.guardar(c);
        Cliente d = new Cliente();
        d.setNombre("Otro Cliente"); d.setCedula("1012345678"); d.setActivo(true);
        assertThrows(RuntimeException.class, () -> clienteDAO.guardar(d));
        assertEquals(1, clienteDAO.listarTodos().size());
    }

    @Test @DisplayName("CP-022 Crear proveedor con datos válidos")
    void cp022() {
        crearProveedor("900123456-1", "Distribuidora Andina SAS");
        Proveedor pr = proveedorDAO.listarTodos().get(0);
        assertEquals("Distribuidora Andina SAS", pr.getRazonSocial());
        assertTrue(pr.isActivo());
    }

    @Test @DisplayName("CP-024 Proveedor con productos asociados reporta la cantidad (regla de bloqueo)")
    void cp024() {
        Proveedor pr = crearProveedor("900123456-1", "Distribuidora Andina SAS");
        crearProducto("P001", "Arroz 500 g", "3500", 50, 10, pr.getId());
        assertEquals(1, proveedorDAO.listarTodos().get(0).getCantidadProductos(),
                "El controlador usa esta cantidad para impedir la eliminación");
    }
}

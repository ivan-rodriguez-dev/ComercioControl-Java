package app.pruebas;

import app.dao.VentaDAO;
import app.model.DetalleVenta;
import app.model.Producto;
import app.model.Usuario;
import app.model.Venta;
import app.util.LicenciaManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RF-12 / RF-13 – Licencias y dashboard")
class DashboardLicenciasPruebasTest extends BaseDatosPrueba {

    @AfterEach
    void limpiarLicencia() {
        System.clearProperty("comerciocontrol.licencia.secreto");
        LicenciaManager.getInstance().recargar();
    }

    @Test @DisplayName("CP-041/042 Sin licencia la app inicia en edición Lite con límites 100 productos y 1 usuario")
    void cp041cp042() {
        LicenciaManager lic = LicenciaManager.getInstance();
        lic.recargar();
        assertTrue(lic.esLite());
        assertEquals(100, LicenciaManager.MAX_PRODUCTOS_LITE);
        assertEquals(1, LicenciaManager.MAX_USUARIOS_LITE);
    }

    @Test @DisplayName("CP-041b Activación de la edición Pro con clave válida")
    void cp041b() {
        System.setProperty("comerciocontrol.licencia.secreto", "secreto-solo-pruebas");
        LicenciaManager lic = LicenciaManager.getInstance();
        lic.recargar();
        assertFalse(lic.activar("Tienda Prueba", "CLAVE-INVALIDA"));
        assertTrue(lic.activar("Tienda Prueba", LicenciaManager.generarClave("Tienda Prueba")));
        assertTrue(lic.esPro());
    }

    @Test @DisplayName("CP-043 Indicadores del dashboard coinciden con la base de datos")
    void cp043() {
        Usuario u = admin();
        Producto a = crearProducto("P001", "Arroz 500 g", "3500", 8, 10, null);   // crítico
        crearProducto("P002", "Aceite 1 L", "12000", 30, 5, null);                 // normal
        Venta v = new Venta();
        v.setUsuarioId(u.getId());
        v.setTotal(new BigDecimal("8330")); v.setDescuento(BigDecimal.ZERO); v.setIva(BigDecimal.ZERO);
        v.getDetalles().add(new DetalleVenta(a.getId(), a.getNombre(), 2, a.getPrecioVenta()));
        new VentaDAO().registrarVenta(v);
        assertEquals(2, productoDAO.contarActivos());
        assertEquals(1, productoDAO.contarCriticos());
        assertEquals(0, new BigDecimal("8330").compareTo(new VentaDAO().totalVentasHoy()));
        double[] r = productoDAO.resumenInventario();
        assertEquals(36, (int) r[1], "Unidades: 6 + 30");
    }
}

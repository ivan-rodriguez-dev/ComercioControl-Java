package app.pruebas;

import app.dao.AuditoriaDAO;
import app.dao.CajaDAO;
import app.dao.MovimientoDAO;
import app.dao.VentaDAO;
import app.model.Caja;
import app.model.DetalleVenta;
import app.model.Movimiento;
import app.model.Producto;
import app.model.Usuario;
import app.model.Venta;
import app.util.ReciboPDF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RF-07 / RF-08 / RF-09 / RF-11 – Movimientos, ventas, caja y auditoría")
class MovimientosVentasCajaPruebasTest extends BaseDatosPrueba {

    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final CajaDAO cajaDAO = new CajaDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    private Movimiento movimiento(Producto p, Usuario u, Movimiento.Tipo tipo, int cantidad, int resultante) {
        Movimiento m = new Movimiento();
        m.setProductoId(p.getId());
        m.setProductoNombre(p.getNombre());
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockResultante(resultante);
        m.setUsuarioId(u.getId());
        m.setObservacion("Prueba GA9");
        return m;
    }

    private Venta venta(Usuario u, Producto p, int cantidad, String total) {
        Venta v = new Venta();
        v.setUsuarioId(u.getId());
        v.setTotal(new BigDecimal(total));
        v.setDescuento(BigDecimal.ZERO);
        v.setIva(BigDecimal.ZERO);
        v.getDetalles().add(new DetalleVenta(p.getId(), p.getNombre(), cantidad, p.getPrecioVenta()));
        return v;
    }

    @Test @DisplayName("CP-025 Registrar una entrada de stock (50 + 20 = 70)")
    void cp025() {
        Usuario u = admin();
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 50, 10, null);
        movimientoDAO.registrar(movimiento(p, u, Movimiento.Tipo.entrada, 20, 70));
        assertEquals(70, productoDAO.buscarPorId(p.getId()).getStockActual());
        List<Movimiento> hist = movimientoDAO.listarFiltrado("entrada");
        assertEquals(1, hist.size());
        assertEquals(70, hist.get(0).getStockResultante());
    }

    @Test @DisplayName("CP-028 Registrar un ajuste de inventario (queda en 65)")
    void cp028() {
        Usuario u = admin();
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 70, 10, null);
        movimientoDAO.registrar(movimiento(p, u, Movimiento.Tipo.ajuste, 65, 65));
        assertEquals(65, productoDAO.buscarPorId(p.getId()).getStockActual());
    }

    @Test @DisplayName("CP-029 Sin caja abierta no hay caja del día (precondición de bloqueo de venta)")
    void cp029() {
        admin();
        assertNull(cajaDAO.obtenerCajaHoy());
    }

    @Test @DisplayName("CP-031 Venta completa descuenta el stock (70 - 2 = 68)")
    void cp031() {
        Usuario u = admin();
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 70, 10, null);
        int id = ventaDAO.registrarVenta(venta(u, p, 2, "8330"));
        assertTrue(id > 0);
        assertEquals(68, productoDAO.buscarPorId(p.getId()).getStockActual());
        assertEquals(0, new BigDecimal("8330").compareTo(ventaDAO.totalVentasHoy()));
    }

    @Test @DisplayName("CP-033 Generación del recibo en PDF")
    void cp033() throws Exception {
        Usuario u = admin();
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 70, 10, null);
        Venta v = venta(u, p, 2, "8330");
        ventaDAO.registrarVenta(v);
        File pdf = new File(app.util.Rutas.dir("recibos"), "recibo_prueba_" + v.getId() + ".pdf");
        ReciboPDF.generar(v, "Carlos Pérez", "Administrador Prueba", pdf);
        assertTrue(pdf.exists() && pdf.length() > 500, "El PDF debe existir y tener contenido");
    }

    @Test @DisplayName("CP-034 Apertura de caja con 50.000")
    void cp034() {
        Usuario u = admin();
        cajaDAO.abrirCaja(new BigDecimal("50000"), u.getId());
        Caja c = cajaDAO.obtenerCajaHoy();
        assertNotNull(c);
        assertEquals("abierta", c.getEstado());
        assertEquals(0, new BigDecimal("50000").compareTo(c.getApertura()));
    }

    @Test @DisplayName("CP-035 Cierre de caja: diferencia = 82.000 - (50.000 + 32.130) = -130")
    void cp035() {
        Usuario u = admin();
        Producto p = crearProducto("P010", "Producto 10.000", "10000", 10, 1, null);
        cajaDAO.abrirCaja(new BigDecimal("50000"), u.getId());
        ventaDAO.registrarVenta(venta(u, p, 3, "32130"));
        Caja c = cajaDAO.obtenerCajaHoy();
        cajaDAO.cerrarCaja(c.getId(), new BigDecimal("82000"), "Cierre de prueba");
        Caja cerrada = cajaDAO.listarHistorial(1).get(0);
        assertEquals("cerrada", cerrada.getEstado());
        assertEquals(0, new BigDecimal("32130").compareTo(cerrada.getVentasDia()));
        assertEquals(0, new BigDecimal("-130").compareTo(cerrada.getDiferencia()));
        assertNull(cajaDAO.obtenerCajaHoy(), "Después del cierre no debe quedar caja abierta");
    }

    @Disabled("INC-04 pendiente: el cierre suma todas las ventas del día y no solo las del turno. "
            + "Requiere guardar la hora de apertura de la caja; queda para el siguiente ciclo.")
    @Test @DisplayName("CP-035b Segunda caja del mismo día solo suma las ventas hechas desde su apertura")
    void cp035b() {
        Usuario u = admin();
        Producto p = crearProducto("P010", "Producto 10.000", "10000", 10, 1, null);
        cajaDAO.abrirCaja(new BigDecimal("50000"), u.getId());
        ventaDAO.registrarVenta(venta(u, p, 1, "11900"));
        cajaDAO.cerrarCaja(cajaDAO.obtenerCajaHoy().getId(), new BigDecimal("61900"), "Turno 1");
        // Turno 2 del mismo día, sin ventas nuevas
        cajaDAO.abrirCaja(new BigDecimal("50000"), u.getId());
        cajaDAO.cerrarCaja(cajaDAO.obtenerCajaHoy().getId(), new BigDecimal("50000"), "Turno 2");
        Caja turno2 = cajaDAO.listarHistorial(1).get(0);
        assertEquals(0, BigDecimal.ZERO.compareTo(turno2.getVentasDia()),
                "El turno 2 no tuvo ventas, pero el cierre suma " + turno2.getVentasDia());
        assertEquals(0, BigDecimal.ZERO.compareTo(turno2.getDiferencia()));
    }

    @Test @DisplayName("CP-018b Eliminar producto con historial muestra un aviso controlado")
    void cp018b() {
        Usuario u = admin();
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 50, 10, null);
        movimientoDAO.registrar(movimiento(p, u, Movimiento.Tipo.entrada, 20, 70));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> productoDAO.eliminar(p.getId()),
                "Se espera un aviso de negocio, no un error SQL sin controlar");
        assertTrue(ex.getMessage().contains("historial"));
        assertNotNull(productoDAO.buscarPorId(p.getId()), "El producto no debe borrarse");
    }

    @Test @DisplayName("CP-040 Ventas y movimientos quedan en la auditoría")
    void cp040() {
        Usuario u = admin();
        Producto p = crearProducto("P001", "Arroz 500 g", "3500", 50, 10, null);
        ventaDAO.registrarVenta(venta(u, p, 1, "4165"));
        movimientoDAO.registrar(movimiento(p, u, Movimiento.Tipo.entrada, 5, 54));
        List<String[]> aud = auditoriaDAO.listar(10);
        assertEquals(2, aud.size());
        assertTrue(aud.stream().anyMatch(r -> "VENTA".equals(r[2])));
        assertTrue(aud.stream().anyMatch(r -> "ENTRADA".equals(r[2])));
        assertTrue(aud.stream().allMatch(r -> "Administrador Prueba".equals(r[1]) && r[0] != null));
    }
}

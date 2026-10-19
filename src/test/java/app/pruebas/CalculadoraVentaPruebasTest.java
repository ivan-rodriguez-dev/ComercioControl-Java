package app.pruebas;

import app.util.CalculadoraVenta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RF-08 – Cálculo de totales de la venta")
class CalculadoraVentaPruebasTest {

    private static void igual(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "Esperado " + esperado + " y se obtuvo " + real);
    }

    @Test @DisplayName("CP-030 Venta con descuento 10 % e IVA 19 %: 30.000 → total 32.130")
    void cp030() {
        CalculadoraVenta.Totales t = CalculadoraVenta.calcular(new BigDecimal("30000"), 10);
        igual("30000", t.subtotal());
        igual("3000", t.descuento());
        igual("27000", t.baseIva());
        igual("5130", t.iva());
        igual("32130", t.total());
    }

    @Test @DisplayName("CP-030b Descuento mayor a 100 % se limita a 100 % (total nunca negativo)")
    void cp030b() {
        double pct = CalculadoraVenta.porcentajeDescuento("150");
        assertEquals(100.0, pct);
        CalculadoraVenta.Totales t = CalculadoraVenta.calcular(new BigDecimal("30000"), pct);
        igual("0", t.total());
        assertTrue(t.iva().signum() >= 0);
    }

    @Test @DisplayName("CP-030c Descuento negativo o no numérico se toma como 0 %")
    void cp030c() {
        assertEquals(0.0, CalculadoraVenta.porcentajeDescuento("-10"));
        assertEquals(0.0, CalculadoraVenta.porcentajeDescuento("abc"));
        assertEquals(12.5, CalculadoraVenta.porcentajeDescuento("12,5"));
        igual("35700", CalculadoraVenta.calcular(new BigDecimal("30000"), CalculadoraVenta.porcentajeDescuento("-10")).total());
    }
}

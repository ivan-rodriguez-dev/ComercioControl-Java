package app.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculo único de los totales de una venta (descuento, base, IVA 19 % y total).
 * La usan tanto la vista previa del carrito como el cobro, para que el valor
 * mostrado y el valor registrado sean siempre el mismo (corrección INC-02).
 */
public final class CalculadoraVenta {

    public static final BigDecimal IVA = new BigDecimal("0.19");

    private CalculadoraVenta() {}

    public record Totales(BigDecimal subtotal, BigDecimal descuento, BigDecimal baseIva,
                          BigDecimal iva, BigDecimal total) {}

    /** Convierte el texto del campo de descuento en un porcentaje válido entre 0 y 100. */
    public static double porcentajeDescuento(String texto) {
        double pct;
        try { pct = Double.parseDouble(texto == null ? "0" : texto.trim().replace(',', '.')); }
        catch (NumberFormatException e) { pct = 0; }
        if (Double.isNaN(pct) || pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        return pct;
    }

    public static Totales calcular(BigDecimal subtotal, double descuentoPct) {
        double pct = Math.max(0, Math.min(100, descuentoPct));
        BigDecimal descuento = subtotal.multiply(BigDecimal.valueOf(pct / 100)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal baseIva = subtotal.subtract(descuento);
        BigDecimal iva = baseIva.multiply(IVA).setScale(0, RoundingMode.HALF_UP);
        return new Totales(subtotal, descuento, baseIva, iva, baseIva.add(iva));
    }
}

package app.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtil {

    private static final Locale LOCALE_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    public static String formatearPrecio(BigDecimal valor) {
        if (valor == null) return "$0";
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_CO);
        nf.setMaximumFractionDigits(0);
        return "$" + nf.format(valor);
    }

    public static String formatearPrecio(double valor) {
        return formatearPrecio(BigDecimal.valueOf(valor));
    }

    public static String formatearFecha(LocalDateTime fecha) {
        if (fecha == null) return "";
        return fecha.format(FMT_FECHA);
    }

    public static String formatearFechaCorta(LocalDateTime fecha) {
        if (fecha == null) return "";
        return fecha.format(FMT_FECHA_CORTA);
    }
}

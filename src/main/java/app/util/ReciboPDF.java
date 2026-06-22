package app.util;

import app.model.DetalleVenta;
import app.model.Venta;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.GrayColor;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera un recibo / comprobante de venta en PDF con formato de tirilla (80 mm).
 */
public class ReciboPDF {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final BaseColor GRIS = new BaseColor(100, 116, 139);
    private static final BaseColor OSCURO = new BaseColor(30, 41, 59);

    private static final Font F_TITULO  = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, OSCURO);
    private static final Font F_SUBT    = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL, GRIS);
    private static final Font F_LABEL   = new Font(Font.FontFamily.HELVETICA, 8,  Font.NORMAL, GRIS);
    private static final Font F_ITEM    = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, OSCURO);
    private static final Font F_TOTAL   = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, OSCURO);
    private static final Font F_FOOTER  = new Font(Font.FontFamily.HELVETICA, 8,  Font.ITALIC, GRIS);

    /**
     * Genera el recibo en el archivo destino indicado.
     */
    public static void generar(Venta venta, String clienteNombre, String cajeroNombre, File destino) throws Exception {
        // Tirilla de 80 mm de ancho ≈ 226 pt. Alto holgado para que fluya si hay muchos ítems.
        Rectangle pagina = new Rectangle(226, 600);
        Document doc = new Document(pagina, 14, 14, 14, 14);
        try (FileOutputStream fos = new FileOutputStream(destino)) {
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            // En la edición gratuita (Lite) el recibo lleva marca de agua.
            if (app.util.LicenciaManager.getInstance().esLite()) {
                writer.setPageEvent(new MarcaAguaLite());
            }
            doc.open();
            escribirContenido(doc, venta, clienteNombre, cajeroNombre);
            // Cerrar el documento antes de que se cierre el stream (iText vuelca el PDF al cerrar).
            doc.close();
        }
    }

    /** Marca de agua diagonal para la edición gratuita (Lite). */
    static class MarcaAguaLite extends PdfPageEventHelper {
        @Override public void onEndPage(PdfWriter writer, Document doc) {
            PdfContentByte cb = writer.getDirectContentUnder();
            Rectangle size = doc.getPageSize();
            Font f = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, new GrayColor(0.88f));
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("ComercioControl Lite", f),
                    size.getWidth() / 2, size.getHeight() / 2, 45);
        }
    }

    private static void escribirContenido(Document doc, Venta venta, String clienteNombre, String cajeroNombre)
            throws DocumentException {

            // Encabezado
            doc.add(centrado("ComercioControl", F_TITULO));
            doc.add(centrado("Recibo de venta", F_SUBT));
            doc.add(separador());

            // Datos de la venta
            LocalDateTime fecha = venta.getFecha() != null ? venta.getFecha() : LocalDateTime.now();
            doc.add(linea("Recibo:",  "#" + venta.getId()));
            doc.add(linea("Fecha:",   fecha.format(FMT)));
            doc.add(linea("Cajero:",  cajeroNombre != null ? cajeroNombre : "—"));
            doc.add(linea("Cliente:", clienteNombre != null && !clienteNombre.isBlank() ? clienteNombre : "Consumidor final"));
            doc.add(separador());

            // Ítems
            PdfPTable tabla = new PdfPTable(new float[]{1.4f, 3.4f, 2.2f});
            tabla.setWidthPercentage(100);
            tabla.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            tabla.addCell(celdaCab("Cant"));
            tabla.addCell(celdaCab("Producto"));
            tabla.addCell(celdaCabDer("Subtotal"));

            for (DetalleVenta d : venta.getDetalles()) {
                tabla.addCell(celdaItem(String.valueOf(d.getCantidad()), Element.ALIGN_LEFT));
                tabla.addCell(celdaItem(d.getProductoNombre()
                        + "\n" + FormatUtil.formatearPrecio(d.getPrecioUnitario()) + " c/u", Element.ALIGN_LEFT));
                tabla.addCell(celdaItem(FormatUtil.formatearPrecio(d.getSubtotal()), Element.ALIGN_RIGHT));
            }
            doc.add(tabla);
            doc.add(separador());

            // Totales
            BigDecimal subtotal = venta.getDetalles().stream()
                    .map(DetalleVenta::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            doc.add(linea("Subtotal:",  FormatUtil.formatearPrecio(subtotal)));
            if (venta.getDescuento() != null && venta.getDescuento().compareTo(BigDecimal.ZERO) > 0)
                doc.add(linea("Descuento:", "-" + FormatUtil.formatearPrecio(venta.getDescuento())));
            doc.add(linea("IVA (19%):", FormatUtil.formatearPrecio(venta.getIva())));
            doc.add(separador());
            doc.add(lineaTotal("TOTAL:", FormatUtil.formatearPrecio(venta.getTotal())));

            doc.add(new Paragraph(" "));
            doc.add(centrado("¡Gracias por su compra!", F_FOOTER));
    }

    private static Paragraph centrado(String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static PdfPTable linea(String etiqueta, String valor) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        t.addCell(celda(etiqueta, F_LABEL, Element.ALIGN_LEFT));
        t.addCell(celda(valor, F_ITEM, Element.ALIGN_RIGHT));
        return t;
    }

    private static PdfPTable lineaTotal(String etiqueta, String valor) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        t.addCell(celda(etiqueta, F_TOTAL, Element.ALIGN_LEFT));
        t.addCell(celda(valor, F_TOTAL, Element.ALIGN_RIGHT));
        return t;
    }

    private static PdfPCell celda(String texto, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setPaddingTop(1);
        c.setPaddingBottom(1);
        return c;
    }

    private static PdfPCell celdaCab(String texto) {
        PdfPCell c = celda(texto, F_LABEL, Element.ALIGN_LEFT);
        c.setPaddingBottom(3);
        return c;
    }

    private static PdfPCell celdaCabDer(String texto) {
        PdfPCell c = celda(texto, F_LABEL, Element.ALIGN_RIGHT);
        c.setPaddingBottom(3);
        return c;
    }

    private static PdfPCell celdaItem(String texto, int align) {
        return celda(texto, F_ITEM, align);
    }

    private static Paragraph separador() {
        Paragraph p = new Paragraph("------------------------------------------", F_SUBT);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(2);
        p.setSpacingAfter(2);
        return p;
    }
}

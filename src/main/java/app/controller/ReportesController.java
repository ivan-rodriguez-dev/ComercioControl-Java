package app.controller;

import app.dao.ProductoDAO;
import app.dao.VentaDAO;
import app.model.Producto;
import app.model.Venta;
import app.util.FormatUtil;
import app.util.LicenciaManager;
import app.util.SessionManager;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportesController {

    @FXML private Label lblHeaderInfo;
    @FXML private Button btnTabVentas;
    @FXML private Button btnTabVendidos;
    @FXML private Button btnTabInventario;
    @FXML private Button btnTabStock;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private HBox panelStats;
    @FXML private TableView<Object> tablaReporte;
    @FXML private StackPane chartContainer;
    @FXML private Canvas canvasChart;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    private enum TabActiva { VENTAS, VENDIDOS, INVENTARIO, STOCK }
    private TabActiva tabActiva = TabActiva.VENTAS;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final List<Button> tabBtns = new ArrayList<>();

    private static final Color C_BLUE  = Color.web("#2563eb");
    private static final Color C_RED   = Color.web("#ef4444");
    private static final Color C_GREEN = Color.web("#10b981");
    private static final Color C_GRAY  = Color.web("#94a3b8");
    private static final Color C_GRID  = Color.web("#e2e8f0");
    private static final Color C_TEXT  = Color.web("#64748b");
    private static final Color C_DARK  = Color.web("#1e293b");

    @FXML
    public void initialize() {
        var usuario = SessionManager.getInstance().getUsuarioActual();
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", new Locale("es")));
        lblHeaderInfo.setText((usuario != null ? usuario.getIniciales() : "")
                + "  " + (usuario != null ? usuario.getNombre() : "") + " · Hoy " + fecha);

        tabBtns.addAll(List.of(btnTabVentas, btnTabVendidos, btnTabInventario, btnTabStock));
        dpDesde.setValue(LocalDate.now().withDayOfMonth(1));
        dpHasta.setValue(LocalDate.now());

        chartContainer.widthProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > 10) Platform.runLater(this::dibujarChart);
        });

        mostrarVentas();
    }

    @FXML public void mostrarVentas()     { tabActiva = TabActiva.VENTAS;     setTabActiva(btnTabVentas);     aplicarFiltro(); }
    @FXML public void mostrarVendidos()   { tabActiva = TabActiva.VENDIDOS;   setTabActiva(btnTabVendidos);   aplicarFiltro(); }
    @FXML public void mostrarInventario() { tabActiva = TabActiva.INVENTARIO; setTabActiva(btnTabInventario); aplicarFiltro(); }
    @FXML public void mostrarStock()      { tabActiva = TabActiva.STOCK;      setTabActiva(btnTabStock);      aplicarFiltro(); }

    private void setTabActiva(Button activo) {
        tabBtns.forEach(b -> {
            b.getStyleClass().remove("btn-primary");
            if (!b.getStyleClass().contains("btn-secondary")) b.getStyleClass().add("btn-secondary");
        });
        activo.getStyleClass().remove("btn-secondary");
        if (!activo.getStyleClass().contains("btn-primary")) activo.getStyleClass().add("btn-primary");
    }

    @FXML public void aplicarFiltro() {
        switch (tabActiva) {
            case VENTAS     -> cargarVentas();
            case VENDIDOS   -> cargarMasVendidos();
            case INVENTARIO -> cargarInventario();
            case STOCK      -> cargarStockCritico();
        }
    }

    // ═══════════════════ VENTAS POR PERÍODO ═══════════════════

    private void cargarVentas() {
        String desde = fecha(dpDesde, LocalDate.now().withDayOfMonth(1));
        String hasta = fecha(dpHasta, LocalDate.now());
        List<Venta> ventas = ventaDAO.listarPorPeriodo(desde, hasta);

        BigDecimal total = ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int n = ventas.size();
        BigDecimal ticket = n > 0
                ? total.divide(BigDecimal.valueOf(n), 0, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        long dias = contarDiasConVentas(ventas);

        panelStats.getChildren().setAll(
                statCard("Ventas totales",   FormatUtil.formatearPrecio(total), "Período seleccionado", "green"),
                statCard("Transacciones",    String.valueOf(n), "Operaciones registradas", ""),
                statCard("Ticket promedio",  FormatUtil.formatearPrecio(ticket), "Por transacción", ""),
                statCard("Días con ventas",  dias + " días", "En el período", "")
        );

        tablaReporte.getColumns().clear();
        tablaReporte.getItems().clear();
        tablaReporte.getColumns().addAll(
            col("Fecha",     o -> FormatUtil.formatearFecha(((Venta)o).getFecha()), 130),
            col("Venta",     o -> "#" + ((Venta)o).getId(), 60),
            col("Unidades",  o -> String.valueOf(((Venta)o).getTotalUnidades()), 70),
            col("Descuento", o -> FormatUtil.formatearPrecio(((Venta)o).getDescuento()), 100),
            col("IVA",       o -> FormatUtil.formatearPrecio(((Venta)o).getIva()), 100),
            col("Total",     o -> FormatUtil.formatearPrecio(((Venta)o).getTotal()), 110),
            col("Vendedor",  o -> ((Venta)o).getUsuarioNombre() != null ? ((Venta)o).getUsuarioNombre() : "—", 100)
        );
        tablaReporte.setItems(FXCollections.observableArrayList(new ArrayList<>(ventas)));
        tablaReporte.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Platform.runLater(this::dibujarChart);
    }

    // ═══════════════════ MÁS VENDIDOS ═══════════════════

    private void cargarMasVendidos() {
        String desde = fecha(dpDesde, LocalDate.now().withDayOfMonth(1));
        String hasta = fecha(dpHasta, LocalDate.now());
        List<Object[]> top = ventaDAO.topProductosVendidos(desde, hasta, 20);

        int totalUnd = top.stream().mapToInt(a -> ((Number)a[1]).intValue()).sum();
        double totalIng = top.stream().mapToDouble(a -> ((Number)a[2]).doubleValue()).sum();

        panelStats.getChildren().setAll(
                statCard("Productos vendidos", String.valueOf(top.size()), "Con al menos 1 venta", ""),
                statCard("Unidades totales",   String.valueOf(totalUnd), "Despachadas en período", ""),
                statCard("Ingresos período",   FormatUtil.formatearPrecio(BigDecimal.valueOf(totalIng)), "Por productos vendidos", "green"),
                statCard("Producto top",       top.isEmpty() ? "—" : truncar((String)top.get(0)[0], 14), "Mayor volumen", "")
        );

        tablaReporte.getColumns().clear();
        tablaReporte.getItems().clear();
        tablaReporte.getColumns().addAll(
            col("Producto",          o -> (String)((Object[])o)[0], 220),
            col("Unidades vendidas", o -> String.valueOf(((Number)((Object[])o)[1]).intValue()), 130),
            col("Ingresos",          o -> FormatUtil.formatearPrecio(BigDecimal.valueOf(((Number)((Object[])o)[2]).doubleValue())), 130)
        );
        tablaReporte.setItems(FXCollections.observableArrayList(new ArrayList<>(top)));
        tablaReporte.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Platform.runLater(this::dibujarChart);
    }

    // ═══════════════════ INVENTARIO ACTUAL ═══════════════════

    private void cargarInventario() {
        List<Producto> productos = productoDAO.listarTodos();

        BigDecimal valorCosto = productos.stream()
                .map(p -> p.getPrecioCosto().multiply(BigDecimal.valueOf(p.getStockActual())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorVenta = productos.stream()
                .map(p -> p.getPrecioVenta().multiply(BigDecimal.valueOf(p.getStockActual())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long criticos = productos.stream().filter(p -> p.getEstado() == Producto.Estado.Critico).count();
        long exceso   = productos.stream().filter(p -> p.getEstado() == Producto.Estado.Exceso).count();

        panelStats.getChildren().setAll(
                statCard("Total productos",    String.valueOf(productos.size()), "En catálogo activo", ""),
                statCard("Valor a costo",      FormatUtil.formatearPrecio(valorCosto), "Inversión actual", ""),
                statCard("Valor a PVP",        FormatUtil.formatearPrecio(valorVenta), "Potencial de venta", "green"),
                statCard("Stock crítico",      criticos + " / Exceso " + exceso, "Ítems fuera de rango", "red")
        );

        tablaReporte.getColumns().clear();
        tablaReporte.getItems().clear();
        tablaReporte.getColumns().addAll(
            col("Producto",   o -> ((Producto)o).getNombre(), 180),
            col("Categoría",  o -> nvl(((Producto)o).getCategoria()), 100),
            col("Stock",      o -> String.valueOf(((Producto)o).getStockActual()), 70),
            col("Mín.",       o -> String.valueOf(((Producto)o).getStockMinimo()), 60),
            col("Estado",     o -> ((Producto)o).getEstado().name(), 80),
            col("P. Costo",   o -> FormatUtil.formatearPrecio(((Producto)o).getPrecioCosto()), 100),
            col("P. Venta",   o -> FormatUtil.formatearPrecio(((Producto)o).getPrecioVenta()), 100),
            col("Valor inv.", o -> FormatUtil.formatearPrecio(
                    ((Producto)o).getPrecioCosto().multiply(BigDecimal.valueOf(((Producto)o).getStockActual()))), 110)
        );
        tablaReporte.setItems(FXCollections.observableArrayList(new ArrayList<>(productos)));
        tablaReporte.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Platform.runLater(this::dibujarChart);
    }

    // ═══════════════════ STOCK CRÍTICO ═══════════════════

    private void cargarStockCritico() {
        List<Producto> todos = productoDAO.listarTodos();
        List<Producto> criticos = todos.stream()
                .filter(p -> p.getEstado() == Producto.Estado.Critico)
                .collect(Collectors.toList());

        long sinStock = criticos.stream().filter(p -> p.getStockActual() == 0).count();
        int faltantes = criticos.stream().mapToInt(p -> p.getStockMinimo() - p.getStockActual()).sum();
        BigDecimal valorReponer = criticos.stream()
                .map(p -> p.getPrecioCosto().multiply(BigDecimal.valueOf(
                        Math.max(0, p.getStockMinimo() - p.getStockActual()))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        panelStats.getChildren().setAll(
                statCard("Alertas activas",    String.valueOf(criticos.size()), "Requieren acción inmediata", "red"),
                statCard("Con stock cero",     String.valueOf(sinStock), "Agotados completamente", "red"),
                statCard("Unidades faltantes", "~" + faltantes, "Para alcanzar stock mínimo", ""),
                statCard("Costo estimado",     "~" + FormatUtil.formatearPrecio(valorReponer), "Para reponer inventario", "")
        );

        tablaReporte.getColumns().clear();
        tablaReporte.getItems().clear();
        tablaReporte.getColumns().addAll(
            col("Producto",      o -> ((Producto)o).getNombre(), 180),
            col("Stock actual",  o -> String.valueOf(((Producto)o).getStockActual()), 90),
            col("Stock mínimo",  o -> String.valueOf(((Producto)o).getStockMinimo()), 90),
            col("Faltante",      o -> String.valueOf(Math.max(0, ((Producto)o).getStockMinimo() - ((Producto)o).getStockActual())), 80),
            col("Costo reponer", o -> FormatUtil.formatearPrecio(
                    ((Producto)o).getPrecioCosto().multiply(BigDecimal.valueOf(
                            Math.max(0, ((Producto)o).getStockMinimo() - ((Producto)o).getStockActual())))), 120)
        );
        tablaReporte.setItems(FXCollections.observableArrayList(new ArrayList<>(criticos)));
        tablaReporte.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Platform.runLater(this::dibujarChart);
    }

    // ═══════════════════ CHART DISPATCHER ═══════════════════

    private void dibujarChart() {
        switch (tabActiva) {
            case VENTAS     -> dibujarChartVentas();
            case VENDIDOS   -> dibujarChartVendidos();
            case INVENTARIO -> dibujarChartInventario();
            case STOCK      -> dibujarChartStock();
        }
    }

    private void dibujarChartVentas() {
        String desde = fecha(dpDesde, LocalDate.now().withDayOfMonth(1));
        String hasta = fecha(dpHasta, LocalDate.now());
        LinkedHashMap<String, Double> datos = ventaDAO.ventasPorDiaPeriodo(desde, hasta);
        if (datos.isEmpty()) { sinDatos("Sin ventas en el período seleccionado"); return; }
        String[] labels = datos.keySet().toArray(new String[0]);
        double[] vals   = datos.values().stream().mapToDouble(Double::doubleValue).toArray();
        barrasVerticales(labels, vals, C_BLUE, "Ventas diarias ($)");
    }

    private void dibujarChartVendidos() {
        String desde = fecha(dpDesde, LocalDate.now().withDayOfMonth(1));
        String hasta = fecha(dpHasta, LocalDate.now());
        List<Object[]> top = ventaDAO.topProductosVendidos(desde, hasta, 8);
        if (top.isEmpty()) { sinDatos("Sin ventas registradas en el período"); return; }
        String[] labels = top.stream().map(a -> truncar((String)a[0], 16)).toArray(String[]::new);
        double[] vals   = top.stream().mapToDouble(a -> ((Number)a[1]).doubleValue()).toArray();
        barrasHorizontales(labels, vals, C_BLUE, "Top productos — unidades vendidas");
    }

    private void dibujarChartInventario() {
        List<Producto> productos = productoDAO.listarTodos();
        LinkedHashMap<String, Integer> porCat = new LinkedHashMap<>();
        for (Producto p : productos) {
            String cat = nvl(p.getCategoria()).isEmpty() ? "Sin cat." : p.getCategoria();
            porCat.merge(cat, p.getStockActual(), Integer::sum);
        }
        if (porCat.isEmpty()) { sinDatos("Sin productos en el inventario"); return; }
        String[] labels = porCat.keySet().stream().map(s -> truncar(s, 12)).toArray(String[]::new);
        double[] vals   = porCat.values().stream().mapToDouble(Integer::doubleValue).toArray();
        barrasVerticales(labels, vals, C_GREEN, "Stock total por categoría (unidades)");
    }

    private void dibujarChartStock() {
        List<Producto> criticos = productoDAO.listarTodos().stream()
                .filter(p -> p.getEstado() == Producto.Estado.Critico)
                .limit(6)
                .collect(Collectors.toList());
        if (criticos.isEmpty()) { sinDatos("✓  Sin alertas de stock crítico"); return; }
        String[] labels = criticos.stream().map(p -> truncar(p.getNombre(), 16)).toArray(String[]::new);
        double[] actual = criticos.stream().mapToDouble(p -> (double)p.getStockActual()).toArray();
        double[] minimo = criticos.stream().mapToDouble(p -> (double)p.getStockMinimo()).toArray();
        barrasDobles(labels, actual, minimo, C_RED, C_GRAY, "Actual", "Mínimo");
    }

    // ═══════════════════ DRAWING PRIMITIVES ═══════════════════

    private GraphicsContext prepChart() {
        double cW = chartContainer.getWidth();
        if (cW > 10) canvasChart.setWidth(cW);
        GraphicsContext gc = canvasChart.getGraphicsContext2D();
        gc.clearRect(0, 0, canvasChart.getWidth(), canvasChart.getHeight());
        return gc;
    }

    private void sinDatos(String msg) {
        GraphicsContext gc = prepChart();
        double W = canvasChart.getWidth(), H = canvasChart.getHeight();
        gc.setFill(C_TEXT);
        gc.setFont(Font.font("Segoe UI", 13));
        gc.fillText(msg, W / 2 - msg.length() * 3.5, H / 2 + 5);
    }

    private void barrasVerticales(String[] labels, double[] vals, Color color, String titulo) {
        GraphicsContext gc = prepChart();
        double W = canvasChart.getWidth(), H = canvasChart.getHeight();
        if (W <= 0 || H <= 0) return;

        double padL = 58, padR = 16, padT = 22, padB = 32;
        double cW = W - padL - padR, cH = H - padT - padB;
        int n = labels.length;

        double maxVal = 0;
        for (double v : vals) if (v > maxVal) maxVal = v;
        if (maxVal == 0) maxVal = 1;

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        gc.setFill(C_TEXT);
        gc.fillText(titulo, padL, 14);

        gc.setFont(Font.font("Segoe UI", 9));
        for (int i = 0; i <= 4; i++) {
            double y = padT + cH - (cH * i / 4.0);
            gc.setStroke(C_GRID); gc.setLineWidth(1);
            gc.strokeLine(padL, y, padL + cW, y);
            double val = maxVal * i / 4.0;
            gc.setFill(C_TEXT);
            gc.fillText(fmtVal(val), 2, y + 3);
        }

        double gap = cW / n, bW = gap * 0.6, bOff = (gap - bW) / 2;
        int step = n > 20 ? (int)Math.ceil(n / 15.0) : 1;

        for (int i = 0; i < n; i++) {
            double pct = vals[i] / maxVal;
            double bH  = Math.max(cH * pct, 2);
            double x   = padL + i * gap + bOff;
            double y   = padT + cH - bH;

            gc.setFill(color.deriveColor(0, 1, 1, 0.2));
            gc.fillRoundRect(x + 2, padT + 2, bW, cH, 5, 5);

            gc.setFill(color);
            gc.fillRoundRect(x, y, bW, bH, 5, 5);

            if (vals[i] > 0) {
                String vl = fmtVal(vals[i]);
                gc.setFill(color);
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 8));
                gc.fillText(vl, x + bW / 2 - vl.length() * 2.2, y - 2);
            }

            gc.setFill(C_TEXT);
            gc.setFont(Font.font("Segoe UI", 9));
            String dl = (i % step == 0) ? labels[i] : "";
            gc.fillText(dl, x + bW / 2 - dl.length() * 2.2, H - 4);
        }

        gc.setStroke(C_GRID); gc.setLineWidth(1.5);
        gc.strokeLine(padL, padT + cH, padL + cW, padT + cH);
    }

    private void barrasHorizontales(String[] labels, double[] vals, Color color, String titulo) {
        GraphicsContext gc = prepChart();
        double W = canvasChart.getWidth(), H = canvasChart.getHeight();
        if (W <= 0 || H <= 0) return;

        double padL = 132, padR = 55, padT = 22, padB = 10;
        double cW = W - padL - padR, cH = H - padT - padB;
        int n = labels.length;

        double maxVal = 0;
        for (double v : vals) if (v > maxVal) maxVal = v;
        if (maxVal == 0) maxVal = 1;

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        gc.setFill(C_TEXT);
        gc.fillText(titulo, padL, 14);

        double gap = cH / n, bH = gap * 0.65, bOff = (gap - bH) / 2;

        for (int i = 0; i < n; i++) {
            double pct = vals[i] / maxVal;
            double bW  = Math.max(cW * pct, 2);
            double y   = padT + i * gap + bOff;

            gc.setFill(C_GRID);
            gc.fillRoundRect(padL, y, cW, bH, 4, 4);

            gc.setFill(color.deriveColor(0, 1, 1.0 - i * 0.04, 1));
            gc.fillRoundRect(padL, y, bW, bH, 4, 4);

            gc.setFill(C_DARK);
            gc.setFont(Font.font("Segoe UI", 10));
            gc.fillText(labels[i], 2, y + bH / 2 + 4);

            String vl = fmtVal(vals[i]);
            gc.setFill(color);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            gc.fillText(vl, padL + bW + 5, y + bH / 2 + 4);
        }
    }

    private void barrasDobles(String[] labels, double[] vals1, double[] vals2,
                               Color c1, Color c2, String leg1, String leg2) {
        GraphicsContext gc = prepChart();
        double W = canvasChart.getWidth(), H = canvasChart.getHeight();
        if (W <= 0 || H <= 0) return;

        double padL = 132, padR = 55, padT = 28, padB = 10;
        double cW = W - padL - padR, cH = H - padT - padB;
        int n = labels.length;

        double maxVal = 0;
        for (double v : vals1) if (v > maxVal) maxVal = v;
        for (double v : vals2) if (v > maxVal) maxVal = v;
        if (maxVal == 0) maxVal = 1;

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        gc.setFill(C_TEXT);
        gc.fillText("Stock actual vs mínimo requerido", padL, 14);
        gc.setFill(c1);  gc.fillRoundRect(W - 140, 5, 10, 10, 3, 3);
        gc.setFont(Font.font("Segoe UI", 9));
        gc.setFill(C_TEXT); gc.fillText(leg1, W - 126, 14);
        gc.setFill(c2);  gc.fillRoundRect(W - 78, 5, 10, 10, 3, 3);
        gc.setFill(C_TEXT); gc.fillText(leg2, W - 64, 14);

        double gap = cH / n, groupH = gap * 0.8;
        double sH  = (groupH - 2) / 2, bOff = (gap - groupH) / 2;

        for (int i = 0; i < n; i++) {
            double pct1 = vals1[i] / maxVal, pct2 = vals2[i] / maxVal;
            double y1   = padT + i * gap + bOff;
            double y2   = y1 + sH + 2;

            gc.setFill(Color.web("#f1f5f9"));
            gc.fillRoundRect(padL, y1, cW, sH, 3, 3);
            gc.fillRoundRect(padL, y2, cW, sH, 3, 3);

            gc.setFill(c1);
            gc.fillRoundRect(padL, y1, Math.max(cW * pct1, 2), sH, 3, 3);
            gc.setFill(c2);
            gc.fillRoundRect(padL, y2, Math.max(cW * pct2, 2), sH, 3, 3);

            gc.setFill(C_DARK);
            gc.setFont(Font.font("Segoe UI", 10));
            gc.fillText(labels[i], 2, y1 + groupH / 2 + 3);

            gc.setFill(c1);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
            gc.fillText(String.format("%.0f", vals1[i]), padL + cW * pct1 + 5, y1 + sH - 1);
            gc.setFill(c2);
            gc.fillText(String.format("%.0f", vals2[i]), padL + cW * pct2 + 5, y2 + sH - 1);
        }
    }

    // ═══════════════════ HELPERS ═══════════════════

    private String fecha(DatePicker dp, LocalDate def) {
        return (dp.getValue() != null ? dp.getValue() : def).format(FMT);
    }

    private String fmtVal(double v) {
        return v >= 1_000_000 ? String.format("%.1fM", v / 1_000_000)
             : v >= 1_000     ? String.format("%.0fK", v / 1_000)
             :                   String.format("%.0f",  v);
    }

    private String truncar(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max - 1) + "…" : (s != null ? s : "");
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private long contarDiasConVentas(List<Venta> ventas) {
        return ventas.stream()
                .map(v -> v.getFecha() != null ? v.getFecha().toLocalDate() : null)
                .filter(Objects::nonNull)
                .distinct().count();
    }

    private TableColumn<Object, String> col(String titulo, java.util.function.Function<Object, String> fn, double width) {
        TableColumn<Object, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cell -> new SimpleStringProperty(fn.apply(cell.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    private VBox statCard(String label, String valor, String sub, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        Label lbl = new Label(label); lbl.getStyleClass().add("stat-label");
        Label val = new Label(valor);
        if ("red".equals(color))        val.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        else if ("green".equals(color)) val.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
        else                            val.getStyleClass().add("stat-value");
        Label sublbl = new Label(sub); sublbl.getStyleClass().add("stat-sub");
        card.getChildren().addAll(lbl, val, sublbl);
        return card;
    }

    private boolean bloqueadoPorLicencia() {
        if (LicenciaManager.getInstance().esPro()) return false;
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Versión Pro");
        a.setHeaderText("La exportación a PDF/Excel está disponible en la versión Pro.");
        a.setContentText(LicenciaManager.MSG_PRO);
        a.showAndWait();
        return true;
    }

    @FXML private void exportarPDF() {
        if (bloqueadoPorLicencia()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("reporte_" + LocalDate.now() + ".pdf");
        File file = fc.showSaveDialog(tablaReporte.getScene().getWindow());
        if (file == null) return;

        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            // Título
            com.itextpdf.text.Font fTitulo = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
            String titulo = switch (tabActiva) {
                case VENTAS     -> "Reporte de Ventas";
                case VENDIDOS   -> "Productos Más Vendidos";
                case INVENTARIO -> "Inventario Actual";
                case STOCK      -> "Stock Crítico";
            };
            doc.add(new Paragraph(titulo + " — " + LocalDate.now(), fTitulo));
            doc.add(new Paragraph(" "));

            // Tabla
            List<TableColumn<Object, ?>> cols = tablaReporte.getColumns();
            PdfPTable tabla = new PdfPTable(cols.size());
            tabla.setWidthPercentage(100);

            com.itextpdf.text.Font fHeader = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 9, com.itextpdf.text.Font.BOLD,
                    BaseColor.WHITE);
            com.itextpdf.text.Font fCell = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA, 8);

            for (TableColumn<Object, ?> col : cols) {
                PdfPCell cell = new PdfPCell(new Phrase(col.getText(), fHeader));
                cell.setBackgroundColor(new BaseColor(37, 99, 235));
                cell.setPadding(5);
                tabla.addCell(cell);
            }

            for (Object item : tablaReporte.getItems()) {
                for (TableColumn<Object, ?> col : cols) {
                    Object val = col.getCellData(item);
                    PdfPCell cell = new PdfPCell(new Phrase(val != null ? val.toString() : "", fCell));
                    cell.setPadding(4);
                    tabla.addCell(cell);
                }
            }
            doc.add(tabla);
            doc.close();

            new Alert(Alert.AlertType.INFORMATION,
                    "PDF generado exitosamente:\n" + file.getAbsolutePath(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al generar PDF: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    @FXML private void exportarExcel() {
        if (bloqueadoPorLicencia()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("reporte_" + LocalDate.now() + ".xlsx");
        File file = fc.showSaveDialog(tablaReporte.getScene().getWindow());
        if (file == null) return;

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Reporte");

            // Header row
            List<TableColumn<Object, ?>> cols = tablaReporte.getColumns();
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            org.apache.poi.ss.usermodel.CellStyle hStyle = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font hFont = wb.createFont();
            hFont.setBold(true);
            hStyle.setFont(hFont);
            hStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
            hStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < cols.size(); i++) {
                org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                cell.setCellValue(cols.get(i).getText());
                cell.setCellStyle(hStyle);
                sheet.setColumnWidth(i, 4000);
            }

            // Data rows
            int rowIdx = 1;
            for (Object item : tablaReporte.getItems()) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < cols.size(); i++) {
                    Object val = cols.get(i).getCellData(item);
                    row.createCell(i).setCellValue(val != null ? val.toString() : "");
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) { wb.write(fos); }

            new Alert(Alert.AlertType.INFORMATION,
                    "Excel generado exitosamente:\n" + file.getAbsolutePath(), ButtonType.OK).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al generar Excel: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }
}

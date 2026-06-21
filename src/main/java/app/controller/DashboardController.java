package app.controller;

import app.dao.ProductoDAO;
import app.dao.VentaDAO;
import app.model.Producto;
import app.util.FormatUtil;
import app.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label lblHeaderInfo;
    @FXML private Label lblVentasHoy;
    @FXML private Label lblVentasHoySub;
    @FXML private Label lblProductos;
    @FXML private Label lblCategorias;
    @FXML private Label lblAlertas;
    @FXML private Label lblAlertasSub;
    @FXML private Label lblIngresosMes;
    @FXML private Label lblIngresosMesSub;
    @FXML private Label lblValorInventario;
    @FXML private Label lblValorInventarioSub;
    @FXML private Canvas canvasGrafico;
    @FXML private VBox panelCritico;
    @FXML private Button btnVerTodos;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final VentaDAO    ventaDAO    = new VentaDAO();

    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final Color COLOR_BARRA       = Color.web("#2563eb");
    private static final Color COLOR_BARRA_MUTED = Color.web("#93c5fd");
    private static final Color COLOR_TEXT        = Color.web("#64748b");
    private static final Color COLOR_GRID        = Color.web("#e2e8f0");

    @FXML private javafx.scene.layout.VBox rootPane;

    @FXML
    public void initialize() {
        cargarEstadisticas();
        cargarStockCritico();
        Platform.runLater(this::dibujarGrafica);
        // Redibuja cuando se cambia el tamaño de ventana
        canvasGrafico.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                canvasGrafico.getParent().layoutBoundsProperty().addListener((o, ov, nv) -> {
                    if (nv.getWidth() > 10) {
                        canvasGrafico.setWidth(nv.getWidth());
                        Platform.runLater(this::dibujarGrafica);
                    }
                });
            }
        });
    }

    // ═══════════════════════ ESTADÍSTICAS ═══════════════════════

    private void cargarEstadisticas() {
        var usuario = SessionManager.getInstance().getUsuarioActual();
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM", new Locale("es", "CO")));
        lblHeaderInfo.setText(
                (usuario != null ? usuario.getIniciales() : "")
                + "  " + (usuario != null ? usuario.getNombre() : "")
                + " · Hoy " + fecha);

        // Ventas hoy
        var ventasHoy = ventaDAO.totalVentasHoy();
        lblVentasHoy.setText(FormatUtil.formatearPrecio(ventasHoy));
        lblVentasHoySub.setText("");

        // Productos
        int totalProductos = productoDAO.contarActivos();
        lblProductos.setText(String.valueOf(totalProductos));
        List<String> cats = productoDAO.listarCategorias();
        lblCategorias.setText(cats.size() + (cats.size() == 1 ? " categoría" : " categorías"));

        // Alertas stock
        int criticos = productoDAO.contarCriticos();
        lblAlertas.setText(String.valueOf(criticos));
        lblAlertasSub.setText(criticos > 0
                ? "Requieren reabastecimiento"
                : "Stock en niveles normales");
        lblAlertas.setStyle(criticos > 0
                ? "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #ef4444;"
                : "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");

        // Ingresos mes
        var ingresosMes = ventaDAO.totalVentasMes();
        lblIngresosMes.setText(FormatUtil.formatearPrecio(ingresosMes));
        lblIngresosMesSub.setText("Mes de " + LocalDate.now()
                .format(DateTimeFormatter.ofPattern("MMMM", new Locale("es", "CO"))));

        // Valor inventario
        double[] resumen = productoDAO.resumenInventario();
        lblValorInventario.setText(FormatUtil.formatearPrecio(resumen[3]));
        lblValorInventarioSub.setText((int) resumen[1] + " uds · costo " + FormatUtil.formatearPrecio(resumen[2]));
    }

    // ═══════════════════════ GRÁFICA DE BARRAS ═══════════════════════

    private void dibujarGrafica() {
        double[] ventas = ventaDAO.ventasPorDiaSemana();

        GraphicsContext gc = canvasGrafico.getGraphicsContext2D();
        double W = canvasGrafico.getWidth();
        double H = canvasGrafico.getHeight();

        if (W <= 0 || H <= 0) return;

        gc.clearRect(0, 0, W, H);

        // Margen
        double padLeft = 55, padRight = 16, padTop = 20, padBottom = 36;
        double chartW = W - padLeft - padRight;
        double chartH = H - padTop - padBottom;

        double maxVal = 0;
        for (double v : ventas) if (v > maxVal) maxVal = v;
        if (maxVal == 0) maxVal = 1;

        // Líneas de cuadrícula y etiquetas Y
        int lines = 4;
        gc.setFont(Font.font("Segoe UI", 10));
        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(1);
        for (int i = 0; i <= lines; i++) {
            double y = padTop + chartH - (chartH * i / lines);
            gc.setStroke(COLOR_GRID);
            gc.strokeLine(padLeft, y, padLeft + chartW, y);
            double val = maxVal * i / lines;
            gc.setFill(COLOR_TEXT);
            String label = val >= 1_000_000 ? String.format("%.1fM", val / 1_000_000)
                         : val >= 1_000     ? String.format("%.0fK", val / 1_000)
                                             : String.format("%.0f", val);
            gc.fillText(label, 0, y + 4);
        }

        // Día de la semana actual (0=Lun … 6=Dom)
        int hoyIdx = LocalDate.now().getDayOfWeek().getValue() - 1;

        // Barras
        int n = 7;
        double barGap  = chartW / n;
        double barW    = barGap * 0.55;
        double barOff  = (barGap - barW) / 2;

        for (int i = 0; i < n; i++) {
            double pct  = ventas[i] / maxVal;
            double barH = chartH * pct;
            double x    = padLeft + i * barGap + barOff;
            double y    = padTop  + chartH - barH;

            // Sombra suave
            gc.setFill(Color.web("#dbeafe", 0.6));
            gc.fillRoundRect(x + 2, padTop + 2, barW, chartH, 6, 6);

            // Barra principal
            gc.setFill(i == hoyIdx ? COLOR_BARRA : COLOR_BARRA_MUTED);
            gc.fillRoundRect(x, y, barW, Math.max(barH, 2), 6, 6);

            // Valor encima
            if (ventas[i] > 0) {
                gc.setFill(i == hoyIdx ? COLOR_BARRA : COLOR_TEXT);
                gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 9));
                String valLabel = ventas[i] >= 1_000_000
                        ? String.format("%.1fM", ventas[i] / 1_000_000)
                        : ventas[i] >= 1_000
                        ? String.format("%.0fK", ventas[i] / 1_000)
                        : String.format("%.0f", ventas[i]);
                gc.fillText(valLabel, x + barW / 2 - valLabel.length() * 2.5, y - 4);
            }

            // Etiqueta día
            gc.setFill(i == hoyIdx ? COLOR_BARRA : COLOR_TEXT);
            gc.setFont(Font.font("Segoe UI", i == hoyIdx ? FontWeight.BOLD : FontWeight.NORMAL, 10));
            String diaLabel = i == hoyIdx ? "Hoy" : DIAS[i];
            gc.fillText(diaLabel, x + barW / 2 - diaLabel.length() * 2.5, H - 6);
        }

        // Eje X
        gc.setStroke(COLOR_GRID);
        gc.setLineWidth(1.5);
        gc.strokeLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH);
    }

    // ═══════════════════════ STOCK CRÍTICO ═══════════════════════

    private void cargarStockCritico() {
        panelCritico.getChildren().clear();

        List<Producto> todos = productoDAO.listarTodos();
        List<Producto> criticos = todos.stream()
                .filter(p -> p.getEstado() == Producto.Estado.Critico)
                .limit(5)
                .collect(Collectors.toList());

        if (criticos.isEmpty()) {
            Label ok = new Label("✓  Sin alertas de stock crítico");
            ok.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 13px;");
            panelCritico.getChildren().add(ok);
        } else {
            for (Producto p : criticos) {
                HBox fila = new HBox(8);
                fila.setAlignment(Pos.CENTER_LEFT);
                fila.setStyle("-fx-padding: 4 0;");

                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 10px;");

                Label nombre = new Label(p.getNombre());
                nombre.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");
                HBox.setHgrow(nombre, Priority.ALWAYS);

                Label stockVal = new Label(String.valueOf(p.getStockActual()));
                stockVal.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");

                Label minVal = new Label("/ mín " + p.getStockMinimo());
                minVal.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

                fila.getChildren().addAll(dot, nombre, stockVal, minVal);
                panelCritico.getChildren().add(fila);
            }
        }

        long totalCriticos = todos.stream()
                .filter(p -> p.getEstado() == Producto.Estado.Critico).count();
        if (totalCriticos > 5) {
            btnVerTodos.setText("Ver todos (" + totalCriticos + ") →");
            btnVerTodos.setVisible(true);
            btnVerTodos.setManaged(true);
        } else {
            btnVerTodos.setVisible(false);
            btnVerTodos.setManaged(false);
        }
    }

    @FXML
    private void verTodosCriticos() {
        try {
            javafx.scene.Node scene = canvasGrafico.getScene().getRoot();
            if (scene instanceof javafx.scene.layout.HBox hbox) {
                hbox.getChildren().stream()
                    .filter(n -> n instanceof javafx.scene.layout.StackPane)
                    .findFirst()
                    .ifPresent(sp -> {
                        try {
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                                    getClass().getResource("/fxml/inventario.fxml"));
                            javafx.scene.Node vista = loader.load();
                            ((javafx.scene.layout.StackPane) sp).getChildren().setAll(vista);
                        } catch (Exception ex) { ex.printStackTrace(); }
                    });
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}

package app.controller;

import app.dao.MovimientoDAO;
import app.dao.ProductoDAO;
import app.model.Movimiento;
import app.model.Producto;
import app.util.FormatUtil;
import app.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MovimientosController {

    @FXML private Label lblHeaderInfo;
    @FXML private ComboBox<String> cbProducto;
    @FXML private ComboBox<String> cbTipo;
    @FXML private TextField txtCantidad;
    @FXML private Label lblStockResultante;
    @FXML private TextArea txtObservacion;
    @FXML private Label lblError;
    @FXML private ComboBox<String> cbFiltroTipo;
    @FXML private TableView<Movimiento> tablaMovimientos;
    @FXML private TableColumn<Movimiento, String> colFecha;
    @FXML private TableColumn<Movimiento, String> colProducto;
    @FXML private TableColumn<Movimiento, String> colTipo;
    @FXML private TableColumn<Movimiento, String> colCant;
    @FXML private TableColumn<Movimiento, String> colStock;
    @FXML private TableColumn<Movimiento, String> colUsuario;
    @FXML private TableColumn<Movimiento, String> colObs;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private List<Producto> productos;

    @FXML
    public void initialize() {
        var usuario = SessionManager.getInstance().getUsuarioActual();
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", new Locale("es")));
        lblHeaderInfo.setText((usuario != null ? usuario.getIniciales() : "")
                + "  " + (usuario != null ? usuario.getNombre() : "") + " · Hoy " + fecha);

        // Tipos de movimiento
        cbTipo.getItems().setAll(
                "📦 Entrada (compra/recepción)",
                "📤 Salida (ajuste manual)",
                "🔧 Ajuste de inventario"
        );
        cbTipo.setValue("📦 Entrada (compra/recepción)");

        // Filtro historial
        cbFiltroTipo.getItems().setAll("Todos los tipos", "entrada", "salida", "ajuste");
        cbFiltroTipo.setValue("Todos los tipos");

        configurarTabla();
        cargarProductos();
        cargarHistorial();
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(
                FormatUtil.formatearFechaCorta(c.getValue().getFecha())));

        colProducto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductoNombre()));
        colProducto.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            }
        });

        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo().name()));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(capitalize(s));
                switch (s) {
                    case "entrada" -> badge.getStyleClass().add("badge-entrada");
                    case "salida"  -> badge.getStyleClass().add("badge-salida");
                    default        -> badge.getStyleClass().add("badge-ajuste");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        colCant.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCantidadFormateada()));
        colCant.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.startsWith("+")
                        ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                        : "-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        });
        colCant.setStyle("-fx-alignment: CENTER;");

        colStock.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStockResultante())));
        colStock.setStyle("-fx-alignment: CENTER;");

        colUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuarioNombre()));
        colObs.setCellValueFactory(c -> {
            String obs = c.getValue().getObservacion();
            return new SimpleStringProperty(obs != null && obs.length() > 20
                    ? obs.substring(0, 20) + "..." : (obs != null ? obs : "—"));
        });

        tablaMovimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarProductos() {
        productos = productoDAO.listarTodos();
        cbProducto.getItems().clear();
        cbProducto.getItems().add("— Selecciona un producto —");
        productos.forEach(p -> cbProducto.getItems().add(
                p.getId() + " | " + p.getNombre() + " (Stock: " + p.getStockActual() + ")"));
        cbProducto.setValue("— Selecciona un producto —");
    }

    @FXML private void onProductoSeleccionado() { recalcularStock(); }

    @FXML void recalcularStock() {
        Producto p = getProductoSeleccionado();
        if (p == null) { lblStockResultante.setText("—"); return; }

        int cantidad = 0;
        try { cantidad = Integer.parseInt(txtCantidad.getText().trim()); } catch (Exception ignored) {}

        String tipoVal = cbTipo.getValue();
        int stockActual = p.getStockActual();
        int resultado;

        if (tipoVal != null && tipoVal.contains("Salida")) {
            resultado = stockActual - cantidad;
        } else if (tipoVal != null && tipoVal.contains("Ajuste")) {
            resultado = cantidad; // ajuste reemplaza
        } else {
            resultado = stockActual + cantidad;
        }

        lblStockResultante.setText(String.valueOf(resultado));
        if (resultado < 0) {
            lblStockResultante.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else if (resultado <= p.getStockMinimo()) {
            lblStockResultante.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            lblStockResultante.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    @FXML private void registrarMovimiento() {
        Producto p = getProductoSeleccionado();
        if (p == null) {
            mostrarError("Selecciona un producto.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            mostrarError("La cantidad debe ser un número positivo.");
            return;
        }

        String tipoVal = cbTipo.getValue();
        Movimiento.Tipo tipo;
        int stockResultante;

        if (tipoVal != null && tipoVal.contains("Salida")) {
            tipo = Movimiento.Tipo.salida;
            stockResultante = p.getStockActual() - cantidad;
        } else if (tipoVal != null && tipoVal.contains("Ajuste")) {
            tipo = Movimiento.Tipo.ajuste;
            stockResultante = cantidad;
        } else {
            tipo = Movimiento.Tipo.entrada;
            stockResultante = p.getStockActual() + cantidad;
        }

        if (stockResultante < 0) {
            mostrarError("Stock insuficiente. Stock actual: " + p.getStockActual());
            return;
        }

        Movimiento m = new Movimiento();
        m.setProductoId(p.getId());
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockResultante(stockResultante);
        m.setUsuarioId(SessionManager.getInstance().getUsuarioActual().getId());
        m.setObservacion(txtObservacion.getText().trim());

        movimientoDAO.registrar(m);

        // Limpiar formulario
        cbProducto.setValue("— Selecciona un producto —");
        txtCantidad.setText("0");
        txtObservacion.clear();
        lblStockResultante.setText("—");
        ocultarError();

        cargarProductos();
        cargarHistorial();
    }

    @FXML private void filtrarHistorial() { cargarHistorial(); }

    private void cargarHistorial() {
        String filtro = cbFiltroTipo.getValue();
        List<Movimiento> lista = movimientoDAO.listarFiltrado(
                filtro != null && !filtro.equals("Todos los tipos") ? filtro : null);
        tablaMovimientos.setItems(FXCollections.observableArrayList(lista));
    }

    private Producto getProductoSeleccionado() {
        String sel = cbProducto.getValue();
        if (sel == null || sel.startsWith("—")) return null;
        try {
            int id = Integer.parseInt(sel.split(" \\| ")[0].trim());
            return productos.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
    private void ocultarError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

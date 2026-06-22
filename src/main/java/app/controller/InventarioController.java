package app.controller;

import app.dao.ProductoDAO;
import app.dao.ProveedorDAO;
import app.model.Producto;
import app.model.Proveedor;
import app.util.FormatUtil;
import app.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class InventarioController {

    @FXML private Label lblHeaderInfo;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cbCategoria;
    @FXML private ComboBox<String> cbEstado;
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colStock;
    @FXML private TableColumn<Producto, String> colStockMin;
    @FXML private TableColumn<Producto, String> colCosto;
    @FXML private TableColumn<Producto, String> colVenta;
    @FXML private TableColumn<Producto, String> colEstado;
    @FXML private TableColumn<Producto, Void> colAcciones;
    @FXML private Label lblStatProductos;
    @FXML private Label lblStatUnidades;
    @FXML private Label lblStatValorCosto;
    @FXML private Label lblStatValorVenta;
    @FXML private Label lblConteo;
    @FXML private Label lblPagina;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private StackPane modalOverlay;
    @FXML private VBox rootPane;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    private List<Producto> todosLosProductos = new ArrayList<>();
    private List<Producto> productosFiltrados = new ArrayList<>();
    private int paginaActual = 0;
    private static final int ITEMS_POR_PAGINA = 10;

    @FXML
    public void initialize() {
        var usuario = SessionManager.getInstance().getUsuarioActual();
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", new Locale("es")));
        lblHeaderInfo.setText((usuario != null ? usuario.getIniciales() : "")
                + "  " + (usuario != null ? usuario.getNombre() : "") + " · Hoy " + fecha);

        configurarTabla();
        cargarFiltros();
        cargarProductos();
        cargarResumen();
    }

    private void cargarResumen() {
        double[] r = productoDAO.resumenInventario();
        lblStatProductos.setText(String.valueOf((int) r[0]));
        lblStatUnidades.setText(String.valueOf((int) r[1]));
        lblStatValorCosto.setText(FormatUtil.formatearPrecio(r[2]));
        lblStatValorVenta.setText(FormatUtil.formatearPrecio(r[3]));
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colCodigo.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        colNombre.setCellValueFactory(c -> {
            Producto p = c.getValue();
            String icono = p.getEstado() == Producto.Estado.Critico ? "⚠ " : "";
            return new SimpleStringProperty(icono + p.getNombre());
        });
        colNombre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                Producto p = getTableView().getItems().get(getIndex());
                if (p.getEstado() == Producto.Estado.Critico)
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 13px;");
                else
                    setStyle("-fx-text-fill: #1e293b; -fx-font-size: 13px;");
            }
        });

        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCategoria.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label chip = new Label(s);
                chip.getStyleClass().add("categoria-chip");
                setGraphic(chip);
                setText(null);
            }
        });

        colStock.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStockActual())));
        colStock.setStyle("-fx-alignment: CENTER;");

        colStockMin.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getStockMinimo())));
        colStockMin.setStyle("-fx-alignment: CENTER;");

        colCosto.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.formatearPrecio(c.getValue().getPrecioCosto())));
        colVenta.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.formatearPrecio(c.getValue().getPrecioVenta())));
        colVenta.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");
            }
        });

        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado().name()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label("● " + s);
                switch (s) {
                    case "Critico" -> badge.getStyleClass().add("badge-critico");
                    case "Exceso"  -> badge.getStyleClass().add("badge-exceso");
                    default        -> badge.getStyleClass().add("badge-normal");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnVer = new Button("👁");
            private final Button btnEdit = new Button("✏");
            private final Button btnDel = new Button("🗑");
            {
                btnVer.getStyleClass().add("btn-icon");
                btnEdit.getStyleClass().add("btn-icon");
                btnDel.getStyleClass().add("btn-danger");
                btnDel.setStyle("-fx-font-size: 12px; -fx-padding: 3 7;");

                btnVer.setOnAction(e -> verProducto(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> editarProducto(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> eliminarProducto(getTableView().getItems().get(getIndex())));
            }
            private final HBox box = new HBox(8, btnVer, btnEdit, btnDel);
            { box.setAlignment(Pos.CENTER); }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablaProductos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarFiltros() {
        cbCategoria.getItems().clear();
        cbCategoria.getItems().add("Todas las categorías");
        cbCategoria.getItems().addAll(productoDAO.listarCategorias());
        cbCategoria.setValue("Todas las categorías");

        cbEstado.getItems().setAll("Todos los estados", "Normal", "Crítico", "Exceso");
        cbEstado.setValue("Todos los estados");
    }

    private void cargarProductos() {
        todosLosProductos = productoDAO.listarTodos();
        aplicarFiltros();
    }

    @FXML private void onBuscar() { paginaActual = 0; aplicarFiltros(); }
    @FXML private void onFiltrar() { paginaActual = 0; aplicarFiltros(); }

    private void aplicarFiltros() {
        String texto = txtBuscar.getText().toLowerCase().trim();
        String cat = cbCategoria.getValue();
        String estado = cbEstado.getValue();

        productosFiltrados = todosLosProductos.stream().filter(p -> {
            boolean matchTexto = texto.isEmpty()
                    || p.getNombre().toLowerCase().contains(texto)
                    || p.getCodigo().toLowerCase().contains(texto)
                    || (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(texto));
            boolean matchCat = cat == null || cat.equals("Todas las categorías")
                    || cat.equals(p.getCategoria());
            boolean matchEstado = estado == null || estado.equals("Todos los estados")
                    || estado.equals("Crítico") && p.getEstado() == Producto.Estado.Critico
                    || estado.equals("Normal") && p.getEstado() == Producto.Estado.Normal
                    || estado.equals("Exceso") && p.getEstado() == Producto.Estado.Exceso;
            return matchTexto && matchCat && matchEstado;
        }).collect(Collectors.toList());

        actualizarPagina();
    }

    private void actualizarPagina() {
        int total = productosFiltrados.size();
        int totalPaginas = (int) Math.ceil((double) total / ITEMS_POR_PAGINA);
        if (paginaActual >= totalPaginas && totalPaginas > 0) paginaActual = totalPaginas - 1;
        if (paginaActual < 0) paginaActual = 0;

        int desde = paginaActual * ITEMS_POR_PAGINA;
        int hasta = Math.min(desde + ITEMS_POR_PAGINA, total);

        ObservableList<Producto> pagina = FXCollections.observableArrayList(
                productosFiltrados.subList(desde, hasta));
        tablaProductos.setItems(pagina);

        lblConteo.setText("Mostrando " + (total == 0 ? 0 : desde + 1) + "–" + hasta + " de " + total + " productos");
        lblPagina.setText(String.valueOf(paginaActual + 1));
        btnPrev.setDisable(paginaActual == 0);
        btnNext.setDisable(paginaActual >= totalPaginas - 1);
    }

    @FXML private void paginaAnterior() { if (paginaActual > 0) { paginaActual--; actualizarPagina(); } }
    @FXML private void paginaSiguiente() { paginaActual++; actualizarPagina(); }

    // ===================== ACCIONES =====================

    private void verProducto(Producto p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle del producto");
        alert.setHeaderText(p.getNombre());
        alert.setContentText(
                "Código: " + p.getCodigo() + "\n" +
                "Categoría: " + p.getCategoria() + "\n" +
                "Stock actual: " + p.getStockActual() + "\n" +
                "Stock mínimo: " + p.getStockMinimo() + "\n" +
                "Precio costo: " + FormatUtil.formatearPrecio(p.getPrecioCosto()) + "\n" +
                "Precio venta: " + FormatUtil.formatearPrecio(p.getPrecioVenta()) + "\n" +
                "Estado: " + p.getEstado().name()
        );
        alert.showAndWait();
    }

    private void editarProducto(Producto p) {
        mostrarFormulario(p);
    }

    private void eliminarProducto(Producto p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar producto");
        confirm.setHeaderText("¿Eliminar \"" + p.getNombre() + "\"?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                productoDAO.eliminar(p.getId());
                cargarProductos();
                cargarFiltros();
                cargarResumen();
            }
        });
    }

    @FXML private void abrirFormNuevo() { mostrarFormulario(null); }

    private void mostrarFormulario(Producto productoEditar) {
        boolean esNuevo = productoEditar == null;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(tablaProductos.getScene().getWindow());
        dialog.setTitle(esNuevo ? "Nuevo producto" : "Editar producto");
        dialog.setResizable(false);

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setPrefWidth(520);

        Label titulo = new Label(esNuevo ? "Nuevo producto" : "Editar producto");
        titulo.setFont(Font.font(18));
        titulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TextField txtCodigo = crearCampo("Código *", esNuevo ? "" : productoEditar.getCodigo());
        TextField txtNombre = crearCampo("Nombre *", esNuevo ? "" : productoEditar.getNombre());

        Label lblCat = new Label("Categoría");
        lblCat.getStyleClass().add("form-label");
        ComboBox<String> cbCat = new ComboBox<>();
        cbCat.setEditable(true);
        cbCat.getItems().addAll(productoDAO.listarCategorias());
        cbCat.getStyleClass().add("combo-custom");
        cbCat.setMaxWidth(Double.MAX_VALUE);
        cbCat.setPrefHeight(38);
        if (!esNuevo && productoEditar.getCategoria() != null)
            cbCat.setValue(productoEditar.getCategoria());
        VBox vbCat = new VBox(4, lblCat, cbCat);

        TextField txtCosto = crearCampo("", esNuevo ? "" : String.valueOf(productoEditar.getPrecioCosto().intValue()));
        TextField txtVenta = crearCampo("", esNuevo ? "" : String.valueOf(productoEditar.getPrecioVenta().intValue()));
        GridPane filaPrecio = gridDoble("Precio costo *", txtCosto, "Precio venta *", txtVenta);

        TextField txtStock    = crearCampo("", esNuevo ? "0" : String.valueOf(productoEditar.getStockActual()));
        TextField txtStockMin = crearCampo("", esNuevo ? "0" : String.valueOf(productoEditar.getStockMinimo()));
        GridPane filaStock = gridDoble("Stock actual", txtStock, "Stock mínimo", txtStockMin);

        Label lblProv = new Label("Proveedor");
        lblProv.getStyleClass().add("form-label");
        ComboBox<String> cbProv = new ComboBox<>();
        cbProv.getItems().add("Sin proveedor");
        List<Proveedor> proveedores = proveedorDAO.listarTodos();
        proveedores.forEach(pv -> cbProv.getItems().add(pv.getId() + " - " + pv.getRazonSocial()));
        cbProv.getStyleClass().add("combo-custom");
        cbProv.setMaxWidth(Double.MAX_VALUE);
        cbProv.setPrefHeight(38);
        cbProv.setValue("Sin proveedor");
        if (!esNuevo && productoEditar.getProveedorId() != null) {
            proveedores.stream()
                    .filter(pv -> pv.getId() == productoEditar.getProveedorId())
                    .findFirst()
                    .ifPresent(pv -> cbProv.setValue(pv.getId() + " - " + pv.getRazonSocial()));
        }
        VBox vbProv = new VBox(4, lblProv, cbProv);

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        lblErr.setVisible(false);
        lblErr.setManaged(false);
        lblErr.setWrapText(true);

        Button btnCancelar = new Button("✕  Cancelar");
        btnCancelar.getStyleClass().add("btn-secondary");
        btnCancelar.setPrefHeight(38);
        btnCancelar.setPrefWidth(140);
        btnCancelar.setOnAction(e -> dialog.close());

        Button btnGuardar = new Button("💾  Guardar");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setPrefHeight(38);
        btnGuardar.setPrefWidth(140);

        HBox botones = new HBox(10, btnCancelar, btnGuardar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(8, 0, 0, 0));

        form.getChildren().addAll(
                titulo,
                crearVBox("Código *", txtCodigo),
                crearVBox("Nombre *", txtNombre),
                vbCat, filaPrecio, filaStock, vbProv, lblErr, botones
        );

        btnGuardar.setOnAction(e -> {
            String codigo = txtCodigo.getText().trim();
            String nombre = txtNombre.getText().trim();
            String categoria = cbCat.getValue() != null ? cbCat.getValue().trim() : "";

            if (codigo.isEmpty() || nombre.isEmpty()) {
                lblErr.setText("Código y nombre son obligatorios.");
                lblErr.setVisible(true);
                lblErr.setManaged(true);
                return;
            }
            double costo, venta;
            int stock, stockMin;
            try {
                costo    = Double.parseDouble(txtCosto.getText().trim());
                venta    = Double.parseDouble(txtVenta.getText().trim());
                stock    = Integer.parseInt(txtStock.getText().trim());
                stockMin = Integer.parseInt(txtStockMin.getText().trim());
            } catch (NumberFormatException ex) {
                lblErr.setText("Los valores numéricos no son válidos.");
                lblErr.setVisible(true);
                lblErr.setManaged(true);
                return;
            }

            Integer provId = null;
            String provSel = cbProv.getValue();
            if (provSel != null && !provSel.equals("Sin proveedor")) {
                try { provId = Integer.parseInt(provSel.split(" - ")[0]); } catch (Exception ignored) {}
            }

            Producto p = esNuevo ? new Producto() : productoEditar;
            p.setCodigo(codigo);
            p.setNombre(nombre);
            p.setCategoria(categoria.isEmpty() ? null : categoria);
            p.setPrecioCosto(BigDecimal.valueOf(costo));
            p.setPrecioVenta(BigDecimal.valueOf(venta));
            p.setStockActual(stock);
            p.setStockMinimo(stockMin);
            p.setProveedorId(provId);

            if (esNuevo) productoDAO.guardar(p);
            else         productoDAO.actualizar(p);

            dialog.close();
            cargarProductos();
            cargarFiltros();
            cargarResumen();
        });

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: white; -fx-border-color: transparent;");

        Scene scene = new Scene(scroll);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.sizeToScene();
        // Foco en el campo Código para poder escanear el código de barras de inmediato
        javafx.application.Platform.runLater(txtCodigo::requestFocus);
        dialog.showAndWait();
    }

    private TextField crearCampo(String placeholder, String valor) {
        TextField tf = new TextField(valor);
        tf.setPromptText(placeholder);
        tf.getStyleClass().add("text-field-custom");
        tf.setPrefHeight(38);
        return tf;
    }

    private VBox crearVBox(String etiqueta, TextField tf) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("form-label");
        VBox vb = new VBox(4, lbl, tf);
        HBox.setHgrow(vb, Priority.ALWAYS);
        return vb;
    }

    private VBox vboxCampo(TextField tf, String etiqueta) {
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("form-label");
        lbl.setMaxWidth(Double.MAX_VALUE);
        VBox vb = new VBox(4, lbl, tf);
        vb.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(vb, Priority.ALWAYS);
        tf.setPrefWidth(Double.MAX_VALUE);
        return vb;
    }

    private GridPane gridDoble(String lbl1, TextField tf1, String lbl2, TextField tf2) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(50);
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.getColumnConstraints().addAll(c, c);
        grid.setMaxWidth(Double.MAX_VALUE);

        Label l1 = new Label(lbl1); l1.getStyleClass().add("form-label");
        Label l2 = new Label(lbl2); l2.getStyleClass().add("form-label");
        tf1.setMaxWidth(Double.MAX_VALUE);
        tf2.setMaxWidth(Double.MAX_VALUE);

        VBox col1 = new VBox(4, l1, tf1);
        VBox col2 = new VBox(4, l2, tf2);
        grid.add(col1, 0, 0);
        grid.add(col2, 1, 0);
        return grid;
    }

    @FXML private void exportar() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Exportar");
        info.setHeaderText("Exportar inventario");
        info.setContentText("Funcionalidad disponible en el módulo de Reportes.");
        info.showAndWait();
    }
}

package app.controller;

import app.dao.ProveedorDAO;
import app.model.Proveedor;
import app.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ProveedoresController {

    @FXML private Label lblHeaderInfo;
    @FXML private TextField txtBuscar;
    @FXML private TableView<Proveedor> tablaProveedores;
    @FXML private TableColumn<Proveedor, String> colNit;
    @FXML private TableColumn<Proveedor, String> colRazon;
    @FXML private TableColumn<Proveedor, String> colContacto;
    @FXML private TableColumn<Proveedor, String> colTelefono;
    @FXML private TableColumn<Proveedor, String> colProductos;
    @FXML private TableColumn<Proveedor, String> colEstado;
    @FXML private TableColumn<Proveedor, Void> colAcciones;
    @FXML private VBox panelDetalle;
    @FXML private VBox panelVacio;
    @FXML private Label lblDetNit;
    @FXML private Label lblDetNombre;
    @FXML private Label lblDetContacto;
    @FXML private Label lblDetTelefono;
    @FXML private Label lblDetEmail;
    @FXML private Label lblDetProductos;
    @FXML private Label lblDetEstado;

    @FXML private HBox rootPane;
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    @FXML
    public void initialize() {
        var usuario = SessionManager.getInstance().getUsuarioActual();
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", new Locale("es")));
        lblHeaderInfo.setText((usuario != null ? usuario.getIniciales() : "")
                + "  " + (usuario != null ? usuario.getNombre() : "") + " · Hoy " + fecha);

        configurarTabla();
        cargarProveedores();
    }

    private void configurarTabla() {
        colNit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNit()));
        colNit.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        colRazon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRazonSocial()));
        colRazon.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");
            }
        });

        colContacto.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getContacto() != null ? c.getValue().getContacto() : "—"));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() != null ? c.getValue().getTelefono() : "—"));

        colProductos.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getCantidadProductos())));
        colProductos.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-alignment: CENTER;");
            }
        });

        colEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("Activo".equals(s) ? "badge-activo" : "badge-inactivo");
                setGraphic(badge);
                setText(null);
            }
        });

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            private final Button btnDel = new Button("🗑");
            {
                btnEdit.getStyleClass().add("btn-icon");
                btnDel.getStyleClass().add("btn-danger");
                btnDel.setStyle("-fx-font-size: 12px; -fx-padding: 3 7;");
                btnEdit.setOnAction(e -> editarProveedor(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> eliminarProveedor(getTableView().getItems().get(getIndex())));
            }
            private final HBox box = new HBox(4, btnEdit, btnDel);
            { box.setAlignment(Pos.CENTER); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablaProveedores.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaProveedores.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, nuevo) -> mostrarDetalle(nuevo));
    }

    private void cargarProveedores() {
        List<Proveedor> lista = proveedorDAO.listarTodos();
        tablaProveedores.setItems(FXCollections.observableArrayList(lista));
    }

    @FXML private void onBuscar() {
        String texto = txtBuscar.getText().trim();
        List<Proveedor> lista = texto.isEmpty()
                ? proveedorDAO.listarTodos()
                : proveedorDAO.buscar(texto);
        tablaProveedores.setItems(FXCollections.observableArrayList(lista));
    }

    private void mostrarDetalle(Proveedor p) {
        if (p == null) {
            panelDetalle.setVisible(false); panelDetalle.setManaged(false);
            panelVacio.setVisible(true); panelVacio.setManaged(true);
            return;
        }
        panelVacio.setVisible(false); panelVacio.setManaged(false);
        panelDetalle.setVisible(true); panelDetalle.setManaged(true);

        lblDetNit.setText("NIT: " + p.getNit());
        lblDetNombre.setText(p.getRazonSocial());
        lblDetContacto.setText("Contacto: " + (p.getContacto() != null ? p.getContacto() : "—"));
        lblDetTelefono.setText("Tel: " + (p.getTelefono() != null ? p.getTelefono() : "—"));
        lblDetEmail.setText("Email: " + (p.getEmail() != null ? p.getEmail() : "—"));
        lblDetProductos.setText("Productos asociados: " + p.getCantidadProductos());
        Label badge = new Label(p.isActivo() ? "Activo" : "Inactivo");
        badge.getStyleClass().add(p.isActivo() ? "badge-activo" : "badge-inactivo");
        lblDetEstado.setGraphic(badge);
        lblDetEstado.setText(null);
    }

    @FXML private void abrirFormNuevo() { mostrarFormulario(null); }
    private void editarProveedor(Proveedor p) { mostrarFormulario(p); }

    private void eliminarProveedor(Proveedor p) {
        if (p.getCantidadProductos() > 0) {
            Alert warn = new Alert(Alert.AlertType.WARNING);
            warn.setTitle("No se puede eliminar");
            warn.setHeaderText("El proveedor tiene " + p.getCantidadProductos() + " productos asociados.");
            warn.setContentText("Primero reasigna o elimina los productos de este proveedor.");
            warn.showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar proveedor");
        confirm.setHeaderText("¿Eliminar \"" + p.getRazonSocial() + "\"?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) { proveedorDAO.eliminar(p.getId()); cargarProveedores(); }
        });
    }

    private void mostrarFormulario(Proveedor editar) {
        boolean esNuevo = editar == null;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(tablaProveedores.getScene().getWindow());
        dialog.setTitle(esNuevo ? "Nuevo proveedor" : "Editar proveedor");
        dialog.setResizable(false);

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setPrefWidth(460);

        Label titulo = new Label(esNuevo ? "Nuevo proveedor" : "Editar proveedor");
        titulo.setFont(Font.font(18));
        titulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TextField txtRazon    = campo("Razón social *", esNuevo ? "" : editar.getRazonSocial());
        TextField txtNit      = campo("NIT *",          esNuevo ? "" : editar.getNit());
        TextField txtContacto = campo("Contacto",       esNuevo ? "" : nvl(editar.getContacto()));
        TextField txtTelefono = campo("Teléfono",       esNuevo ? "" : nvl(editar.getTelefono()));
        TextField txtEmail    = campo("Correo",         esNuevo ? "" : nvl(editar.getEmail()));

        CheckBox chkActivo = new CheckBox("Proveedor activo");
        chkActivo.setSelected(esNuevo || editar.isActivo());
        chkActivo.setStyle("-fx-font-size: 13px;");

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

        form.getChildren().addAll(titulo,
                vb("Razón social *", txtRazon), vb("NIT *", txtNit),
                vb("Contacto", txtContacto), vb("Teléfono", txtTelefono),
                vb("Correo", txtEmail), chkActivo, lblErr, botones);

        btnGuardar.setOnAction(e -> {
            String razon = txtRazon.getText().trim();
            String nit   = txtNit.getText().trim();
            if (razon.isEmpty() || nit.isEmpty()) {
                lblErr.setText("Razón social y NIT son obligatorios.");
                lblErr.setVisible(true);
                lblErr.setManaged(true);
                return;
            }
            Proveedor p = esNuevo ? new Proveedor() : editar;
            p.setRazonSocial(razon);
            p.setNit(nit);
            p.setContacto(txtContacto.getText().trim());
            p.setTelefono(txtTelefono.getText().trim());
            p.setEmail(txtEmail.getText().trim());
            p.setActivo(chkActivo.isSelected());
            if (esNuevo) proveedorDAO.guardar(p);
            else         proveedorDAO.actualizar(p);
            dialog.close();
            cargarProveedores();
        });

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: white; -fx-border-color: transparent;");

        Scene scene = new Scene(scroll);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        dialog.setScene(scene);
        dialog.sizeToScene();
        dialog.showAndWait();
    }

    private TextField campo(String prompt, String valor) {
        TextField tf = new TextField(valor);
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field-custom");
        tf.setPrefHeight(38);
        return tf;
    }
    private VBox vb(String lbl, TextField tf) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label");
        VBox v = new VBox(4, l, tf); tf.setPrefWidth(Double.MAX_VALUE);
        return v;
    }
    private String nvl(String s) { return s != null ? s : ""; }
}

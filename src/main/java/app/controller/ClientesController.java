package app.controller;

import app.dao.ClienteDAO;
import app.model.Cliente;
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

import java.util.List;

public class ClientesController {

    @FXML private TableView<Cliente> tablaClientes;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colCedula;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colEmail;
    @FXML private TableColumn<Cliente, String> colActivo;
    @FXML private TableColumn<Cliente, Void>   colAccion;
    @FXML private TextField txtBuscar;
    @FXML private VBox rootPane;

    private final ClienteDAO clienteDAO = new ClienteDAO();

    @FXML public void initialize() {
        configurarTabla();
        cargarClientes();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colCedula.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getCedula())));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getTelefono())));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getEmail())));
        colActivo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActivo() ? "Activo" : "Inactivo"));
        colActivo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("Activo".equals(s) ? "badge-activo" : "badge-inactivo");
                setGraphic(badge); setText(null);
            }
        });
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏");
            { btnEdit.getStyleClass().add("btn-icon");
              btnEdit.setOnAction(e -> editarCliente(getTableView().getItems().get(getIndex()))); }
            private final HBox box = new HBox(btnEdit);
            { box.setAlignment(Pos.CENTER); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });
        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarClientes() {
        String filtro = txtBuscar != null ? txtBuscar.getText().trim() : "";
        List<Cliente> lista = filtro.isEmpty() ? clienteDAO.listarTodos() : clienteDAO.buscar(filtro);
        tablaClientes.setItems(FXCollections.observableArrayList(lista));
    }

    @FXML private void onBuscar() { cargarClientes(); }
    @FXML private void abrirFormNuevo() { mostrarFormulario(null); }
    private void editarCliente(Cliente c) { mostrarFormulario(c); }

    private void mostrarFormulario(Cliente editar) {
        boolean esNuevo = editar == null;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(tablaClientes.getScene().getWindow());
        dialog.setTitle(esNuevo ? "Nuevo cliente" : "Editar cliente");
        dialog.setResizable(false);

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setPrefWidth(460);

        Label titulo = new Label(esNuevo ? "Nuevo cliente" : "Editar cliente");
        titulo.setFont(Font.font(18));
        titulo.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TextField txtNombre    = campo("Nombre completo *", esNuevo ? "" : nvl(editar.getNombre()));
        TextField txtCedula    = campo("Cédula / NIT",      esNuevo ? "" : nvl(editar.getCedula()));
        TextField txtTelefono  = campo("Teléfono",          esNuevo ? "" : nvl(editar.getTelefono()));
        TextField txtEmail     = campo("Email",             esNuevo ? "" : nvl(editar.getEmail()));
        TextField txtDireccion = campo("Dirección",         esNuevo ? "" : nvl(editar.getDireccion()));

        CheckBox chkActivo = new CheckBox("Cliente activo");
        chkActivo.setSelected(esNuevo || editar.isActivo());
        chkActivo.setStyle("-fx-font-size: 13px;");

        Label lblErr = new Label();
        lblErr.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        lblErr.setVisible(false);
        lblErr.setManaged(false);
        lblErr.setWrapText(true);

        Button btnCancel = new Button("✕  Cancelar");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setPrefHeight(38);
        btnCancel.setPrefWidth(140);
        btnCancel.setOnAction(e -> dialog.close());

        Button btnGuardar = new Button("💾  Guardar");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setPrefHeight(38);
        btnGuardar.setPrefWidth(140);

        HBox botones = new HBox(10, btnCancel, btnGuardar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(8, 0, 0, 0));

        form.getChildren().addAll(titulo,
                vb("Nombre completo *", txtNombre),
                vb("Cédula / NIT", txtCedula),
                vb("Teléfono", txtTelefono),
                vb("Email", txtEmail),
                vb("Dirección", txtDireccion),
                chkActivo, lblErr, botones);

        btnGuardar.setOnAction(e -> {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                lblErr.setText("El nombre es obligatorio.");
                lblErr.setVisible(true);
                lblErr.setManaged(true);
                return;
            }
            Cliente c = esNuevo ? new Cliente() : editar;
            c.setNombre(nombre);
            c.setCedula(txtCedula.getText().trim());
            c.setTelefono(txtTelefono.getText().trim());
            c.setEmail(txtEmail.getText().trim());
            c.setDireccion(txtDireccion.getText().trim());
            c.setActivo(chkActivo.isSelected());
            try {
                if (esNuevo) clienteDAO.guardar(c);
                else         clienteDAO.actualizar(c);
                dialog.close();
                cargarClientes();
            } catch (Exception ex) {
                lblErr.setText("Error: " + ex.getMessage());
                lblErr.setVisible(true);
                lblErr.setManaged(true);
            }
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

    private TextField campo(String prompt, String val) {
        TextField tf = new TextField(val);
        tf.setPromptText(prompt); tf.getStyleClass().add("text-field-custom"); tf.setPrefHeight(38);
        return tf;
    }
    private VBox vb(String lbl, TextField tf) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label");
        tf.setPrefWidth(Double.MAX_VALUE);
        return new VBox(4, l, tf);
    }
    private String nvl(String s) { return s != null ? s : ""; }
}

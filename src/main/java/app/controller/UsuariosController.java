package app.controller;

import app.dao.UsuarioDAO;
import app.model.Usuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class UsuariosController {

    @FXML private TableView<Usuario> tablaUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colCedula;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colActivo;
    @FXML private TableColumn<Usuario, Void>   colAccion;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        configurarTabla();
        cargarUsuarios();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuario()));
        colUsuario.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("-fx-font-family: monospace; -fx-text-fill: #64748b; -fx-font-size: 12px;");
            }
        });

        colCedula.setCellValueFactory(c -> {
            String ced = c.getValue().getCedula();
            return new SimpleStringProperty(ced != null ? ced : "—");
        });

        colRol.setCellValueFactory(c -> new SimpleStringProperty(capitalize(c.getValue().getRol().name())));
        colRol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                String color = switch (s.toLowerCase()) {
                    case "administrador" -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
                    case "vendedor"      -> "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8;";
                    default              -> "-fx-background-color: #f0fdf4; -fx-text-fill: #166534;";
                };
                badge.setStyle(color + "-fx-background-radius: 10; -fx-padding: 2 8; -fx-font-size: 11px;");
                setGraphic(badge); setText(null);
            }
        });

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
            private final Button btnPwd  = new Button("🔑");
            {
                btnEdit.getStyleClass().add("btn-icon");
                btnPwd.getStyleClass().add("btn-icon");
                btnEdit.setOnAction(e -> editarUsuario(getTableView().getItems().get(getIndex())));
                btnPwd.setOnAction(e  -> cambiarPassword(getTableView().getItems().get(getIndex())));
            }
            private final HBox box = new HBox(8, btnEdit, btnPwd);
            { box.setAlignment(Pos.CENTER); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        tablaUsuarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarUsuarios() {
        List<Usuario> lista = usuarioDAO.listarTodos();
        tablaUsuarios.setItems(FXCollections.observableArrayList(lista));
    }

    @FXML private void abrirFormNuevo() { abrirRegistro(null); }
    private void editarUsuario(Usuario u) { abrirRegistro(u); }

    private void abrirRegistro(Usuario editar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/registro.fxml"));
            Parent root = loader.load();

            RegistroController ctrl = loader.getController();
            ctrl.setOnGuardado(this::cargarUsuarios);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(tablaUsuarios.getScene().getWindow());
            dialog.setTitle(editar == null ? "Nuevo usuario" : "Editar usuario");
            dialog.setResizable(false);
            ctrl.setStage(dialog);

            if (editar != null) ctrl.setUsuarioEditar(editar);

            Scene scene = new Scene(root);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cambiarPassword(Usuario u) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Cambiar contraseña");
        dialog.setHeaderText("Nueva contraseña para: " + u.getNombre());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField pfNueva    = new PasswordField(); pfNueva.setPromptText("Nueva contraseña");
        PasswordField pfConfirma = new PasswordField(); pfConfirma.setPromptText("Confirmar contraseña");
        VBox box = new VBox(10,
                new Label("Nueva contraseña:"), pfNueva,
                new Label("Confirmar:"), pfConfirma);
        box.setStyle("-fx-padding: 12;");
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? pfNueva.getText() : null);
        dialog.showAndWait().ifPresent(pass -> {
            if (pass.isBlank()) return;
            if (!pass.equals(pfConfirma.getText())) {
                new Alert(Alert.AlertType.ERROR, "Las contraseñas no coinciden.").showAndWait();
                return;
            }
            usuarioDAO.actualizarPassword(u.getId(), UsuarioDAO.md5(pass));
        });
    }

    private String capitalize(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

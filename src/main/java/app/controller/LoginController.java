package app.controller;

import app.Main;
import app.dao.UsuarioDAO;
import app.model.Usuario;
import app.util.ConfigNegocio;
import app.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.File;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnIngresar;
    @FXML private Label lblNombreNegocioLogin;
    @FXML private Label lblSloganLogin;
    @FXML private ImageView imgLogoLogin;
    @FXML private SVGPath svgLogoLogin;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        aplicarBranding();
        txtPassword.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) onIngresar();
        });
        txtUsuario.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) txtPassword.requestFocus();
        });
    }

    private void aplicarBranding() {
        ConfigNegocio cfg = ConfigNegocio.getInstance();
        cfg.recargar();
        lblNombreNegocioLogin.setText(cfg.getNombreVisible());
        lblSloganLogin.setText(cfg.getSloganVisible());
        boolean tieneLogo = cfg.tieneLogoVisible();
        if (tieneLogo) {
            imgLogoLogin.setImage(new Image(new File(cfg.getLogoPath()).toURI().toString()));
        }
        imgLogoLogin.setVisible(tieneLogo);
        imgLogoLogin.setManaged(tieneLogo);
        svgLogoLogin.setVisible(!tieneLogo);
        svgLogoLogin.setManaged(!tieneLogo);
    }

    @FXML
    private void onIngresar() {
        String usuario  = txtUsuario.getText().trim();
        String password = txtPassword.getText();

        if (usuario.isEmpty() || password.isEmpty()) {
            mostrarError("Por favor completa todos los campos.");
            return;
        }

        btnIngresar.setDisable(true);
        btnIngresar.setText("Verificando...");

        Usuario u = usuarioDAO.autenticar(usuario, password);

        if (u != null) {
            SessionManager.getInstance().iniciarSesion(u);
            abrirDashboard();
        } else {
            mostrarError("Usuario o contraseña incorrectos.");
            txtPassword.clear();
            txtPassword.requestFocus();
            btnIngresar.setDisable(false);
            btnIngresar.setText("Ingresar");
        }
    }

    @FXML
    private void irARegistro() {
        Stage s = stage != null ? stage : (Stage) btnIngresar.getScene().getWindow();
        Main.mostrarRegistro(s);
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void abrirDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            HBox root = loader.load();
            Scene scene = new Scene(root);
            Stage s = stage != null ? stage : (Stage) btnIngresar.getScene().getWindow();
            s.setScene(scene);
            s.setTitle(ConfigNegocio.getInstance().getNombre()
                    + " — " + SessionManager.getInstance().getUsuarioActual().getNombre());
            s.setResizable(true);
            s.setMaximized(true);
        } catch (Exception e) {
            mostrarError("Error al cargar el sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

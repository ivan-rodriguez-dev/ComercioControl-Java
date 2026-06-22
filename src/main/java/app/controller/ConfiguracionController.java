package app.controller;

import app.model.Usuario;
import app.util.ConfigNegocio;
import app.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ConfiguracionController {

    @FXML private SVGPath svgLogoGrande;
    @FXML private ImageView imgLogoGrande;
    @FXML private Label lblNombreNegocio;
    @FXML private Label lblSloganNegocio;
    @FXML private Label lblAdminNombre;
    @FXML private Label lblAdminUsuario;

    @FXML private TextField txtNombre;
    @FXML private TextField txtSlogan;
    @FXML private ImageView imgPreview;
    @FXML private Label lblArchivo;
    @FXML private Label lblMensaje;

    private File logoSeleccionado;     // nuevo logo elegido en esta sesión (aún sin guardar)
    private boolean quitarLogoFlag;    // el usuario pidió quitar el logo
    private MainController mainController;

    public void setMainController(MainController mc) { this.mainController = mc; }

    @FXML
    public void initialize() {
        ConfigNegocio cfg = ConfigNegocio.getInstance();
        cfg.recargar();

        txtNombre.setText(cfg.getNombre());
        txtSlogan.setText(cfg.getSlogan());
        lblNombreNegocio.setText(cfg.getNombre());
        lblSloganNegocio.setText(cfg.getSlogan());
        mostrarLogoActual();

        Usuario u = SessionManager.getInstance().getUsuarioActual();
        if (u != null) {
            lblAdminNombre.setText(u.getNombre());
            lblAdminUsuario.setText("@" + u.getUsuario() + " · " + capitalize(u.getRol().name()));
        }
    }

    @FXML private void seleccionarLogo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar logo del negocio");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(txtNombre.getScene().getWindow());
        if (f == null) return;

        logoSeleccionado = f;
        quitarLogoFlag = false;
        Image img = new Image(f.toURI().toString());
        imgPreview.setImage(img);
        mostrarImagenGrande(img);
        lblArchivo.setText(f.getName());
    }

    @FXML private void quitarLogo() {
        logoSeleccionado = null;
        quitarLogoFlag = true;
        imgPreview.setImage(null);
        mostrarFallbackGrande();
        lblArchivo.setText("(se quitará al guardar)");
    }

    @FXML private void guardar() {
        String nombre = txtNombre.getText().trim();
        String slogan = txtSlogan.getText().trim();

        if (nombre.isEmpty()) {
            mensaje("El nombre del negocio es obligatorio.", true);
            return;
        }

        String logoPathParam = null; // null = mantener el actual
        try {
            if (logoSeleccionado != null) {
                File dir = new File("config");
                if (!dir.exists()) dir.mkdirs();
                String ext = extension(logoSeleccionado.getName());
                File dest = new File(dir, "logo_" + System.currentTimeMillis() + (ext.isEmpty() ? "" : "." + ext));
                Files.copy(logoSeleccionado.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logoPathParam = dest.getPath();
            } else if (quitarLogoFlag) {
                logoPathParam = ""; // limpiar
            }
        } catch (Exception ex) {
            mensaje("No se pudo guardar el logo: " + ex.getMessage(), true);
            return;
        }

        ConfigNegocio.getInstance().guardar(nombre, slogan, logoPathParam);
        logoSeleccionado = null;
        quitarLogoFlag = false;

        lblNombreNegocio.setText(nombre);
        lblSloganNegocio.setText(slogan);
        lblArchivo.setText("");
        mostrarLogoActual();

        if (mainController != null) mainController.aplicarBranding();
        mensaje("✓ Cambios guardados correctamente.", false);
    }

    // ───────── helpers ─────────

    private void mostrarLogoActual() {
        ConfigNegocio cfg = ConfigNegocio.getInstance();
        if (cfg.tieneLogo()) {
            Image img = new Image(new File(cfg.getLogoPath()).toURI().toString());
            imgPreview.setImage(img);
            mostrarImagenGrande(img);
        } else {
            imgPreview.setImage(null);
            mostrarFallbackGrande();
        }
    }

    private void mostrarImagenGrande(Image img) {
        imgLogoGrande.setImage(img);
        imgLogoGrande.setVisible(true);  imgLogoGrande.setManaged(true);
        svgLogoGrande.setVisible(false); svgLogoGrande.setManaged(false);
    }

    private void mostrarFallbackGrande() {
        imgLogoGrande.setImage(null);
        imgLogoGrande.setVisible(false); imgLogoGrande.setManaged(false);
        svgLogoGrande.setVisible(true);  svgLogoGrande.setManaged(true);
    }

    private void mensaje(String texto, boolean error) {
        lblMensaje.setText(texto);
        lblMensaje.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (error ? "#dc2626" : "#16a34a") + ";");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private String extension(String nombreArchivo) {
        int i = nombreArchivo.lastIndexOf('.');
        return i >= 0 ? nombreArchivo.substring(i + 1).toLowerCase() : "";
    }

    private String capitalize(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

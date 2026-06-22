package app.controller;

import app.model.Usuario;
import app.util.ConfigNegocio;
import app.util.LicenciaManager;
import app.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
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

    @FXML private Label lblEdicion;
    @FXML private Label lblTitular;
    @FXML private Label lblBloqueoPro;
    @FXML private Button btnActivarPro;
    @FXML private Button btnDesactivarPro;
    @FXML private Button btnSeleccionarLogo;
    @FXML private Button btnQuitarLogo;
    @FXML private Button btnGuardar;

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
        lblNombreNegocio.setText(cfg.getNombreVisible());
        lblSloganNegocio.setText(cfg.getSloganVisible());
        mostrarLogoActual();

        Usuario u = SessionManager.getInstance().getUsuarioActual();
        if (u != null) {
            lblAdminNombre.setText(u.getNombre());
            lblAdminUsuario.setText("@" + u.getUsuario() + " · " + capitalize(u.getRol().name()));
        }

        actualizarLicenciaUI();
    }

    private void actualizarLicenciaUI() {
        LicenciaManager lic = LicenciaManager.getInstance();
        lic.recargar();
        boolean pro = lic.esPro();

        lblEdicion.setText(pro ? "Pro" : "Lite (gratuita)");
        lblEdicion.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (pro ? "#16a34a" : "#64748b") + ";");

        if (pro && lic.getTitular() != null) {
            lblTitular.setText("Licencia a nombre de: " + lic.getTitular());
            lblTitular.setVisible(true);  lblTitular.setManaged(true);
        } else {
            lblTitular.setVisible(false); lblTitular.setManaged(false);
        }

        btnActivarPro.setVisible(!pro);   btnActivarPro.setManaged(!pro);
        btnDesactivarPro.setVisible(pro); btnDesactivarPro.setManaged(pro);

        boolean bloqueado = !pro;
        txtNombre.setDisable(bloqueado);
        txtSlogan.setDisable(bloqueado);
        btnSeleccionarLogo.setDisable(bloqueado);
        btnQuitarLogo.setDisable(bloqueado);
        btnGuardar.setDisable(bloqueado);
        lblBloqueoPro.setVisible(bloqueado);
        lblBloqueoPro.setManaged(bloqueado);
    }

    @FXML private void activarPro() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Activar versión Pro");
        d.setHeaderText("Ingresa el titular y la clave de tu licencia.");
        d.initOwner(txtNombre.getScene().getWindow());
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField tfTitular = new TextField(txtNombre.getText().trim());
        tfTitular.setPromptText("Titular de la licencia");
        TextField tfClave = new TextField();
        tfClave.setPromptText("CCPRO-XXXXX-XXXXX-XXXXX-XXXXX");
        VBox box = new VBox(8, new Label("Titular:"), tfTitular, new Label("Clave:"), tfClave);
        box.setStyle("-fx-padding: 12;");
        d.getDialogPane().setContent(box);

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            boolean ok = LicenciaManager.getInstance().activar(tfTitular.getText(), tfClave.getText());
            if (ok) {
                refrescarTrasLicencia();
                mensaje("✓ ¡Versión Pro activada! Gracias por tu compra.", false);
            } else {
                mensaje("Clave inválida para ese titular. Verifica los datos.", true);
            }
        });
    }

    @FXML private void desactivarPro() {
        Alert c = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Volver a la versión Lite?", ButtonType.OK, ButtonType.CANCEL);
        c.setHeaderText("Desactivar Pro");
        c.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            LicenciaManager.getInstance().desactivar();
            refrescarTrasLicencia();
            mensaje("Se volvió a la versión Lite.", false);
        });
    }

    private void refrescarTrasLicencia() {
        actualizarLicenciaUI();
        ConfigNegocio cfg = ConfigNegocio.getInstance();
        lblNombreNegocio.setText(cfg.getNombreVisible());
        lblSloganNegocio.setText(cfg.getSloganVisible());
        mostrarLogoActual();
        if (mainController != null) mainController.aplicarBranding();
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
        if (LicenciaManager.getInstance().esLite()) {
            mensaje("La personalización está disponible en la versión Pro.", true);
            return;
        }
        String nombre = txtNombre.getText().trim();
        String slogan = txtSlogan.getText().trim();

        if (nombre.isEmpty()) {
            mensaje("El nombre del negocio es obligatorio.", true);
            return;
        }

        String logoPathParam = null; // null = mantener el actual
        try {
            if (logoSeleccionado != null) {
                File dir = app.util.Rutas.dir("config");
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
        if (cfg.tieneLogoVisible()) {
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

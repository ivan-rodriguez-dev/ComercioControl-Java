package app.controller;

import app.Main;
import app.dao.UsuarioDAO;
import app.model.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class RegistroController {

    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtCedula;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cbRol;
    @FXML private VBox vbRol;
    @FXML private Label lblInfoUsuario;
    @FXML private HBox boxInfoUsuario;
    @FXML private Label lblError;
    @FXML private Label lblPrimerAdmin;
    @FXML private Hyperlink lnkIniciarSesion;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // Modo dialog (desde UsuariosController)
    private Stage dialogStage;
    private Runnable onGuardado;
    private Usuario usuarioEditar;

    // Modo standalone (desde startup)
    private Stage appStage;
    private boolean modoStandalone = false;

    @FXML
    public void initialize() {
        cbRol.getItems().setAll("vendedor", "bodeguero", "administrador");
        cbRol.setValue("vendedor");
    }

    /** Llamado desde Main / LoginController para modo startup */
    public void initStandaloneMode(Stage stage) {
        this.appStage  = stage;
        this.modoStandalone = true;
        lnkIniciarSesion.setVisible(true);
        lnkIniciarSesion.setManaged(true);

        boolean esPrimero = usuarioDAO.listarTodos().isEmpty();
        if (esPrimero) {
            // Primer usuario siempre es administrador
            vbRol.setVisible(false);
            vbRol.setManaged(false);
            lblPrimerAdmin.setVisible(true);
            lblPrimerAdmin.setManaged(true);
        }
    }

    /** Llamado desde UsuariosController para modo dialog */
    public void setStage(Stage stage)          { this.dialogStage = stage; }
    public void setOnGuardado(Runnable cb)     { this.onGuardado  = cb; }

    public void setUsuarioEditar(Usuario u) {
        this.usuarioEditar = u;
        if (u == null) return;
        String[] partes = splitNombreApellido(u.getNombre());
        txtNombres.setText(partes[0]);
        txtApellidos.setText(partes[1]);
        txtCedula.setText(u.getCedula() != null ? u.getCedula() : "");
        if (u.getFechaNacimiento() != null && !u.getFechaNacimiento().isEmpty()) {
            try { dpFecha.setValue(java.time.LocalDate.parse(u.getFechaNacimiento())); }
            catch (Exception ignored) {}
        }
        cbRol.setValue(u.getRol().name());
        actualizarUsuarioGenerado();
    }

    @FXML
    private void actualizarUsuarioGenerado() {
        String nombres   = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        if (nombres.isEmpty() || apellidos.isEmpty()) {
            boxInfoUsuario.setVisible(false);
            boxInfoUsuario.setManaged(false);
            return;
        }
        String cedula = txtCedula.getText().trim();
        String passInfo = cedula.isEmpty() ? "(tu cédula)" : cedula;

        if (usuarioEditar == null) {
            String generado = generarUsuario(nombres, apellidos);
            lblInfoUsuario.setText("Usuario de acceso: " + generado
                    + "  ·  Contraseña temporal: " + passInfo);
        } else {
            lblInfoUsuario.setText("Usuario de acceso: " + usuarioEditar.getUsuario());
        }
        boxInfoUsuario.setVisible(true);
        boxInfoUsuario.setManaged(true);
    }

    @FXML
    private void guardar() {
        String nombres   = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String cedula    = txtCedula.getText().trim();

        if (nombres.isEmpty() || apellidos.isEmpty() || cedula.isEmpty() || dpFecha.getValue() == null) {
            mostrarError("Todos los campos marcados con * son obligatorios.");
            return;
        }

        String nombreCompleto = nombres + " " + apellidos;
        String fecha = dpFecha.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE);

        if (usuarioEditar == null) {
            boolean esPrimero = usuarioDAO.listarTodos().isEmpty();
            String rolElegido = esPrimero ? "administrador" : cbRol.getValue();

            String username = generarUsuarioUnico(nombres, apellidos);
            Usuario u = new Usuario();
            u.setNombre(nombreCompleto);
            u.setUsuario(username);
            u.setPasswordHash(UsuarioDAO.md5(cedula));
            u.setRol(Usuario.Rol.valueOf(rolElegido));
            u.setActivo(true);
            u.setCedula(cedula);
            u.setFechaNacimiento(fecha);

            try {
                usuarioDAO.guardar(u);
            } catch (Exception ex) {
                mostrarError("Error al guardar: " + ex.getMessage());
                return;
            }

            if (modoStandalone) {
                // Mostrar credenciales al primer admin antes de ir al login
                if (esPrimero) {
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Cuenta creada");
                    info.setHeaderText("¡Cuenta de administrador creada!");
                    info.setContentText(
                            "Usuario: " + username + "\n" +
                            "Contraseña: " + cedula + "\n\n" +
                            "Guarda estos datos para iniciar sesión.");
                    info.showAndWait();
                }
                Main.mostrarLogin(appStage);
                return;
            }
        } else {
            usuarioEditar.setNombre(nombreCompleto);
            usuarioEditar.setCedula(cedula);
            usuarioEditar.setFechaNacimiento(fecha);
            usuarioEditar.setRol(Usuario.Rol.valueOf(cbRol.getValue()));
            try {
                usuarioDAO.actualizar(usuarioEditar);
            } catch (Exception ex) {
                mostrarError("Error al actualizar: " + ex.getMessage());
                return;
            }
        }

        if (onGuardado != null) onGuardado.run();
        if (dialogStage != null) dialogStage.close();
    }

    @FXML
    private void cancelar() {
        if (modoStandalone && appStage != null) {
            Main.mostrarLogin(appStage);
        } else if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void irALogin() {
        if (appStage != null) Main.mostrarLogin(appStage);
        else if (dialogStage != null) dialogStage.close();
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private String generarUsuario(String nombres, String apellidos) {
        String primerNombre   = normalizar(nombres.trim().split("\\s+")[0]);
        String primerApellido = normalizar(apellidos.trim().split("\\s+")[0]);
        String base = (primerNombre.isEmpty() ? "" : String.valueOf(primerNombre.charAt(0))) + primerApellido;
        return base.isEmpty() ? "usuario" : base;
    }

    private String generarUsuarioUnico(String nombres, String apellidos) {
        String base = generarUsuario(nombres, apellidos);
        if (!usuarioDAO.existeUsuario(base)) return base;
        for (int i = 2; i <= 99; i++) {
            String c = base + i;
            if (!usuarioDAO.existeUsuario(c)) return c;
        }
        return base + System.currentTimeMillis();
    }

    private String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private String[] splitNombreApellido(String full) {
        if (full == null || full.isBlank()) return new String[]{"", ""};
        String[] p = full.trim().split("\\s+");
        if (p.length == 1) return new String[]{p[0], ""};
        int mid = p.length / 2;
        return new String[]{
            String.join(" ", Arrays.copyOfRange(p, 0, mid)),
            String.join(" ", Arrays.copyOfRange(p, mid, p.length))
        };
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

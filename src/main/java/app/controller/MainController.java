package app.controller;

import app.Main;
import app.model.Usuario;
import app.util.ConfigNegocio;
import app.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class MainController {

    @FXML private StackPane contenidoPrincipal;
    @FXML private Label lblNombreUsuario;
    @FXML private Label lblRolUsuario;
    @FXML private Label lblIniciales;
    @FXML private Button btnAdminUsuarios;
    @FXML private Button btnAuditoria;
    @FXML private Button btnConfiguracion;

    @FXML private ImageView imgLogoNegocio;
    @FXML private SVGPath svgLogoNegocio;
    @FXML private Label lblTituloNegocio;
    @FXML private Label lblSubtituloNegocio;

    @FXML private Button btnDashboard;
    @FXML private Button btnInventario;
    @FXML private Button btnVentas;
    @FXML private Button btnClientes;
    @FXML private Button btnMovimientos;
    @FXML private Button btnProveedores;
    @FXML private Button btnCaja;
    @FXML private Button btnReportes;

    private List<Button> navBotones;

    @FXML
    public void initialize() {
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        if (u != null) {
            lblNombreUsuario.setText(u.getNombre());
            lblRolUsuario.setText(capitalize(u.getRol().name()));
            lblIniciales.setText(u.getIniciales());
        }

        navBotones = List.of(btnDashboard, btnInventario, btnVentas, btnClientes,
                btnMovimientos, btnProveedores, btnCaja, btnReportes,
                btnAdminUsuarios, btnAuditoria, btnConfiguracion);

        boolean esAdmin = SessionManager.getInstance().esAdministrador();
        for (Button b : new Button[]{btnAdminUsuarios, btnAuditoria, btnConfiguracion}) {
            b.setVisible(esAdmin);
            b.setManaged(esAdmin);
        }

        configurarPermisos(u != null ? u.getRol() : null);
        aplicarBranding();

        mostrarDashboard();
    }

    /** Refresca el nombre, eslogan y logo del negocio en el sidebar y el título de la ventana. */
    public void aplicarBranding() {
        ConfigNegocio cfg = ConfigNegocio.getInstance();
        cfg.recargar();

        lblTituloNegocio.setText(cfg.getNombre());
        lblSubtituloNegocio.setText(cfg.getSlogan());

        boolean tieneLogo = cfg.tieneLogo();
        if (tieneLogo) {
            imgLogoNegocio.setImage(new Image(new File(cfg.getLogoPath()).toURI().toString()));
        }
        imgLogoNegocio.setVisible(tieneLogo);
        imgLogoNegocio.setManaged(tieneLogo);
        svgLogoNegocio.setVisible(!tieneLogo);
        svgLogoNegocio.setManaged(!tieneLogo);

        if (contenidoPrincipal.getScene() != null
                && contenidoPrincipal.getScene().getWindow() instanceof Stage stage) {
            Usuario u = SessionManager.getInstance().getUsuarioActual();
            stage.setTitle(cfg.getNombre() + (u != null ? " — " + u.getNombre() : ""));
        }
    }

    /**
     * Aplica visibilidad de módulos según el rol:
     *  - administrador: todos los módulos
     *  - vendedor: Dashboard, Ventas (POS), Clientes y Caja
     *  - bodeguero: Dashboard, Inventario, Movimientos y Proveedores
     */
    private void configurarPermisos(Usuario.Rol rol) {
        if (rol == null || rol == Usuario.Rol.administrador) return;

        Set<Button> permitidos = (rol == Usuario.Rol.vendedor)
                ? Set.of(btnDashboard, btnVentas, btnClientes, btnCaja)
                : Set.of(btnDashboard, btnInventario, btnMovimientos, btnProveedores);

        for (Button b : navBotones) {
            boolean ok = permitidos.contains(b);
            b.setVisible(ok);
            b.setManaged(ok);
        }
    }

    @FXML public void cerrarSesion() {
        SessionManager.getInstance().cerrarSesion();
        Stage stage = (Stage) contenidoPrincipal.getScene().getWindow();
        stage.setMaximized(false);
        stage.setResizable(false);
        stage.setTitle("ComercioControl");
        Main.mostrarLogin(stage);
        stage.centerOnScreen();
    }

    @FXML public void mostrarDashboard()   { cargar("/fxml/dashboard.fxml",  btnDashboard); }
    @FXML public void mostrarInventario()  { cargar("/fxml/inventario.fxml",  btnInventario); }
    @FXML public void mostrarVentas()      { cargar("/fxml/pos.fxml",         btnVentas); }
    @FXML public void mostrarClientes()    { cargar("/fxml/clientes.fxml",    btnClientes); }
    @FXML public void mostrarMovimientos() { cargar("/fxml/movimientos.fxml", btnMovimientos); }
    @FXML public void mostrarProveedores() { cargar("/fxml/proveedores.fxml", btnProveedores); }
    @FXML public void mostrarCaja()        { cargar("/fxml/caja.fxml",         btnCaja); }
    @FXML public void mostrarReportes()    { cargar("/fxml/reportes.fxml",     btnReportes); }

    @FXML public void gestionarUsuarios() {
        if (SessionManager.getInstance().esAdministrador()) cargar("/fxml/usuarios.fxml", btnAdminUsuarios);
    }

    @FXML public void mostrarAuditoria() {
        if (SessionManager.getInstance().esAdministrador()) cargar("/fxml/auditoria.fxml", btnAuditoria);
    }

    @FXML public void mostrarConfiguracion() {
        if (SessionManager.getInstance().esAdministrador()) cargar("/fxml/configuracion.fxml", btnConfiguracion);
    }

    private void cargar(String fxmlPath, Button botonActivo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node vista = loader.load();
            if (loader.getController() instanceof ConfiguracionController cc) cc.setMainController(this);
            contenidoPrincipal.getChildren().setAll(vista);

            // Actualizar estilos del nav
            navBotones.forEach(b -> {
                b.getStyleClass().remove("nav-button-active");
                if (!b.getStyleClass().contains("nav-button")) b.getStyleClass().add("nav-button");
            });
            if (botonActivo != null) {
                botonActivo.getStyleClass().add("nav-button-active");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}

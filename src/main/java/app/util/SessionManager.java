package app.util;

import app.model.Usuario;

public class SessionManager {
    private static SessionManager instancia;
    private Usuario usuarioActual;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instancia == null) instancia = new SessionManager();
        return instancia;
    }

    public void iniciarSesion(Usuario u) { this.usuarioActual = u; }
    public void cerrarSesion() { this.usuarioActual = null; }

    public Usuario getUsuarioActual() { return usuarioActual; }
    public boolean isAutenticado() { return usuarioActual != null; }

    public boolean esAdministrador() {
        return usuarioActual != null && usuarioActual.getRol() == Usuario.Rol.administrador;
    }
}

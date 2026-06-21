package app.model;

public class Usuario {
    private int id;
    private String nombre;
    private String usuario;
    private String passwordHash;
    private Rol rol;
    private boolean activo;
    private String cedula;
    private String fechaNacimiento;

    public enum Rol {
        administrador, vendedor, bodeguero
    }

    public Usuario() {}

    public Usuario(int id, String nombre, String usuario, String passwordHash, Rol rol, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getIniciales() {
        if (nombre == null || nombre.isEmpty()) return "??";
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length >= 2) return ("" + partes[0].charAt(0) + partes[1].charAt(0)).toUpperCase();
        return nombre.substring(0, Math.min(2, nombre.length())).toUpperCase();
    }

    @Override
    public String toString() {
        return nombre + " (" + (rol != null ? rol.name() : "") + ")";
    }
}

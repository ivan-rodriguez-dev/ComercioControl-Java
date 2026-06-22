package app.util;

import app.dao.ConfiguracionDAO;

import java.io.File;

/**
 * Mantiene en caché los datos de personalización del negocio (nombre, eslogan y logo)
 * para que cualquier vista pueda leerlos sin tocar la BD repetidamente.
 */
public class ConfigNegocio {

    public static final String K_NOMBRE = "negocio_nombre";
    public static final String K_SLOGAN = "negocio_slogan";
    public static final String K_LOGO   = "negocio_logo";

    private static ConfigNegocio instancia;

    private final ConfiguracionDAO dao = new ConfiguracionDAO();
    private String nombre;
    private String slogan;
    private String logoPath;

    private ConfigNegocio() { recargar(); }

    public static ConfigNegocio getInstance() {
        if (instancia == null) instancia = new ConfigNegocio();
        return instancia;
    }

    public void recargar() {
        nombre   = dao.get(K_NOMBRE, "ComercioControl");
        slogan   = dao.get(K_SLOGAN, "Tienda Retail");
        logoPath = dao.get(K_LOGO, null);
    }

    public String getNombre()   { return nombre; }
    public String getSlogan()   { return slogan; }
    public String getLogoPath() { return logoPath; }

    public boolean tieneLogo() {
        return logoPath != null && !logoPath.isBlank() && new File(logoPath).exists();
    }

    public void guardar(String nombre, String slogan, String logoPath) {
        dao.set(K_NOMBRE, nombre);
        dao.set(K_SLOGAN, slogan);
        if (logoPath != null) dao.set(K_LOGO, logoPath);
        recargar();
    }
}

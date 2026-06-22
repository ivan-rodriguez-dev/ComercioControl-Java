package app.util;

import java.io.File;

/**
 * Rutas de datos de la aplicación. Todo lo que la app escribe (base de datos,
 * recibos, logo del negocio, licencia) va a una carpeta de datos del usuario
 * (%APPDATA%\ComercioControl en Windows) para que funcione aunque la app esté
 * instalada en una carpeta de solo lectura como Archivos de programa.
 */
public class Rutas {

    private static File baseDir;

    public static File base() {
        if (baseDir == null) {
            String appdata = System.getenv("APPDATA");
            File b = (appdata != null && !appdata.isBlank())
                    ? new File(appdata, "ComercioControl")
                    : new File(System.getProperty("user.home"), ".comerciocontrol");
            b.mkdirs();
            baseDir = b;
        }
        return baseDir;
    }

    /** Archivo dentro de la carpeta de datos. */
    public static File archivo(String nombre) {
        return new File(base(), nombre);
    }

    /** Subcarpeta dentro de la carpeta de datos (se crea si no existe). */
    public static File dir(String nombre) {
        File d = new File(base(), nombre);
        d.mkdirs();
        return d;
    }

    /** URL JDBC de la base de datos SQLite (con barras normales para Windows). */
    public static String dbUrl() {
        return "jdbc:sqlite:" + new File(base(), "comerciocontrol.db").getAbsolutePath().replace('\\', '/');
    }
}

package app.util;

import app.dao.ConfiguracionDAO;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Determina la edición activa (LITE/PRO) y gestiona la activación de la licencia Pro.
 *
 * La clave Pro va ligada al nombre del titular: clave = HMAC-SHA256(SECRETO, titular).
 * Así cada cliente recibe una clave única atada a su nombre. El mismo algoritmo
 * (ver generarClave) se usa para emitir las claves desde la herramienta privada.
 *
 * Nota: el secreto va embebido en la app; como Java es decompilable, esto NO es
 * seguridad fuerte, sino una barrera razonable para el mercado objetivo.
 */
public class LicenciaManager {

    // ── Límites de la edición LITE ──
    public static final int MAX_PRODUCTOS_LITE = 100;
    public static final int MAX_USUARIOS_LITE  = 1;

    public static final String MSG_PRO =
            "Esta función está disponible en la versión Pro.\n"
          + "Puedes activarla en «Mi Negocio» → Licencia.";

    private static final String K_TITULAR = "licencia_titular";
    private static final String K_CLAVE   = "licencia_clave";

    // Cámbialo por tu propio secreto y mantenlo en privado.
    private static final String SECRETO = "C0merc10C0ntr0l::2026::cl4v3-pr1v4d4";

    private static LicenciaManager instancia;

    private final ConfiguracionDAO dao = new ConfiguracionDAO();
    private Edicion edicion = Edicion.LITE;
    private String titular;

    private LicenciaManager() { recargar(); }

    public static LicenciaManager getInstance() {
        if (instancia == null) instancia = new LicenciaManager();
        return instancia;
    }

    public void recargar() {
        String t = dao.get(K_TITULAR, null);
        String c = dao.get(K_CLAVE, null);
        if (t != null && c != null && c.equalsIgnoreCase(generarClave(t))) {
            edicion = Edicion.PRO;
            titular = t;
        } else {
            edicion = Edicion.LITE;
            titular = null;
        }
    }

    public Edicion getEdicion() { return edicion; }
    public boolean esPro()  { return edicion == Edicion.PRO; }
    public boolean esLite() { return edicion == Edicion.LITE; }
    public String getTitular() { return titular; }

    /** Intenta activar Pro con el titular y la clave. Devuelve true si la clave es válida. */
    public boolean activar(String titular, String clave) {
        if (titular == null || clave == null) return false;
        String t = titular.trim();
        if (t.isEmpty()) return false;
        if (!clave.trim().equalsIgnoreCase(generarClave(t))) return false;
        dao.set(K_TITULAR, t);
        dao.set(K_CLAVE, generarClave(t));
        recargar();
        return esPro();
    }

    /**
     * Si el instalador dejó una activación pendiente (activacion.properties en la
     * carpeta de datos), la aplica una sola vez y borra el archivo.
     */
    public void importarActivacionPendiente() {
        File f = Rutas.archivo("activacion.properties");
        if (!f.exists()) return;
        try (FileInputStream in = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            String t = p.getProperty("titular");
            String c = p.getProperty("clave");
            if (t != null && c != null && !t.isBlank() && !c.isBlank()) {
                activar(t, c);
            }
        } catch (Exception ignored) {
        }
        f.delete();
    }

    /** Quita la activación Pro (vuelve a LITE). */
    public void desactivar() {
        dao.set(K_TITULAR, "");
        dao.set(K_CLAVE, "");
        recargar();
    }

    /** Genera la clave determinística para un titular. Úsalo también para emitir claves. */
    public static String generarClave(String titular) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(titular.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
            String b32 = base32(h).substring(0, 20);
            return "CCPRO-" + b32.substring(0, 5) + "-" + b32.substring(5, 10)
                    + "-" + b32.substring(10, 15) + "-" + b32.substring(15, 20);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String base32(byte[] data) {
        final String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(AB.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) sb.append(AB.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }
}

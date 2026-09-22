import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Herramienta PRIVADA para emitir claves Pro. NO la distribuyas con la app.
 * Usa el mismo secreto configurado que app.util.LicenciaManager.
 *
 * Uso (Java 11+):
 *   java tools/GeneradorLicencia.java "Nombre del Titular"
 *
 * El "titular" es el texto al que queda atada la clave (p. ej. el nombre del
 * negocio o del cliente). El cliente debe escribir EXACTAMENTE ese titular más
 * la clave en la app para activar Pro.
 */
public class GeneradorLicencia {

    private static final String ENV_SECRETO = "COMERCIOCONTROL_LICENCIA_SECRETO";

    public static void main(String[] args) throws Exception {
        String secreto = obtenerSecreto();
        if (secreto == null || args.length == 0) {
            System.out.println("Uso: define COMERCIOCONTROL_LICENCIA_SECRETO y ejecuta java tools/GeneradorLicencia.java \"Nombre del Titular\"");
            return;
        }
        String titular = String.join(" ", args);
        System.out.println("Titular: " + titular);
        System.out.println("Clave  : " + generar(titular, secreto));
    }

    static String generar(String titular, String secreto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] h = mac.doFinal(titular.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
        String b32 = base32(h).substring(0, 20);
        return "CCPRO-" + b32.substring(0, 5) + "-" + b32.substring(5, 10)
                + "-" + b32.substring(10, 15) + "-" + b32.substring(15, 20);
    }

    private static String obtenerSecreto() {
        String secreto = System.getenv(ENV_SECRETO);
        return secreto == null || secreto.isBlank() ? null : secreto;
    }

    static String base32(byte[] data) {
        final String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) { sb.append(AB.charAt((buffer >> (bits - 5)) & 31)); bits -= 5; }
        }
        if (bits > 0) sb.append(AB.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }
}

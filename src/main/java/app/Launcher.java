package app;

/**
 * Punto de entrada para ejecutar la app desde el IDE (IntelliJ, Eclipse, etc.).
 *
 * Al NO extender javafx.application.Application, evita el error
 * "JavaFX runtime components are missing" que aparece al ejecutar Main directamente
 * con JavaFX en el classpath. Simplemente delega en Main.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}

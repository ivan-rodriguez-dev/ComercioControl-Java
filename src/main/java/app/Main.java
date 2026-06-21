package app;

import app.controller.LoginController;
import app.controller.RegistroController;
import app.dao.UsuarioDAO;
import app.db.InicializarDB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        InicializarDB.inicializar();

        cargarIcono(stage);

        boolean hayUsuarios = !new UsuarioDAO().listarTodos().isEmpty();

        if (hayUsuarios) {
            mostrarLogin(stage);
        } else {
            mostrarRegistro(stage);
        }

        stage.setTitle("ComercioControl");
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    /** Carga el ícono de la aplicación si está disponible en /img/icono.png. */
    private static void cargarIcono(Stage stage) {
        try (var is = Main.class.getResourceAsStream("/img/icono.png")) {
            if (is != null) stage.getIcons().add(new javafx.scene.image.Image(is));
        } catch (Exception ignored) {}
    }

    public static void mostrarLogin(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            LoginController ctrl = loader.getController();
            ctrl.setStage(stage);
            Scene scene = new Scene(root, 900, 540);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar login: " + e.getMessage(), e);
        }
    }

    public static void mostrarRegistro(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/registro.fxml"));
            Parent root = loader.load();
            RegistroController ctrl = loader.getController();
            ctrl.initStandaloneMode(stage);
            Scene scene = new Scene(root, 920, 560);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            stage.setScene(scene);
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar registro: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

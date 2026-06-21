package app.controller;

import app.dao.AuditoriaDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class AuditoriaController {

    @FXML private TableView<String[]> tablaAuditoria;
    @FXML private TableColumn<String[], String> colFecha;
    @FXML private TableColumn<String[], String> colUsuario;
    @FXML private TableColumn<String[], String> colAccion;
    @FXML private TableColumn<String[], String> colTabla;
    @FXML private TableColumn<String[], String> colDetalle;
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cbAccion;
    @FXML private Label lblTotal;

    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();
    private List<String[]> todos = List.of();

    @FXML public void initialize() {
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        colUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        colAccion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        colTabla.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        colDetalle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));
        tablaAuditoria.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recargar();
    }

    @FXML public void recargar() {
        todos = auditoriaDAO.listar(500);
        List<String> acciones = todos.stream()
                .map(r -> r[2]).distinct().sorted().collect(Collectors.toList());
        acciones.add(0, "Todas las acciones");
        cbAccion.setItems(FXCollections.observableArrayList(acciones));
        cbAccion.setValue("Todas las acciones");
        aplicarFiltro();
    }

    @FXML private void onBuscar() { aplicarFiltro(); }

    private void aplicarFiltro() {
        String texto = txtBuscar != null ? txtBuscar.getText().trim().toLowerCase() : "";
        String accion = cbAccion != null && cbAccion.getValue() != null
                && !cbAccion.getValue().equals("Todas las acciones") ? cbAccion.getValue() : null;

        List<String[]> filtrados = todos.stream()
                .filter(r -> accion == null || r[2].equals(accion))
                .filter(r -> texto.isEmpty()
                        || r[1].toLowerCase().contains(texto)
                        || r[2].toLowerCase().contains(texto)
                        || (r[3] != null && r[3].toLowerCase().contains(texto))
                        || (r[4] != null && r[4].toLowerCase().contains(texto)))
                .collect(Collectors.toList());

        tablaAuditoria.setItems(FXCollections.observableArrayList(filtrados));
        if (lblTotal != null) lblTotal.setText(filtrados.size() + " registros");
    }
}

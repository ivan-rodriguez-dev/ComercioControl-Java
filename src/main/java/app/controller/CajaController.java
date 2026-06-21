package app.controller;

import app.dao.CajaDAO;
import app.model.Caja;
import app.util.FormatUtil;
import app.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CajaController {

    @FXML private Label lblHeaderInfo;
    @FXML private Label lblEstadoCaja;
    @FXML private Label lblFechaCaja;
    @FXML private Label lblVentasDia;
    @FXML private Label lblTransacciones;
    @FXML private Label lblMontApertura;
    @FXML private Label lblDiferencia;
    @FXML private Label lblDiferenciaSub;

    @FXML private VBox panelApertura;
    @FXML private VBox panelCierre;
    @FXML private TextField txtMontoApertura;
    @FXML private TextField txtMontoCierre;
    @FXML private Label lblVentasCierre;
    @FXML private Label lblDiferenciaPreview;
    @FXML private TextArea txtObservacion;

    @FXML private TableView<Caja> tablaHistorial;
    @FXML private TableColumn<Caja, String> colFecha;
    @FXML private TableColumn<Caja, String> colApertura;
    @FXML private TableColumn<Caja, String> colVentas;
    @FXML private TableColumn<Caja, String> colCierre;
    @FXML private TableColumn<Caja, String> colDiferencia;
    @FXML private TableColumn<Caja, String> colEstado;
    @FXML private TableColumn<Caja, String> colUsuario;

    private final CajaDAO cajaDAO = new CajaDAO();
    private Caja cajaActual = null;

    @FXML public void initialize() {
        var usuario = SessionManager.getInstance().getUsuarioActual();
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", new Locale("es")));
        lblHeaderInfo.setText((usuario != null ? usuario.getIniciales() : "")
                + "  " + (usuario != null ? usuario.getNombre() : "") + " · Hoy " + fecha);

        configurarTabla();
        recargar();
    }

    @FXML public void recargar() {
        cajaActual = cajaDAO.obtenerCajaHoy();
        BigDecimal ventasDia = cajaDAO.totalVentasHoy();
        int transacciones = cajaDAO.contarVentasHoy();

        lblVentasDia.setText(FormatUtil.formatearPrecio(ventasDia));
        lblTransacciones.setText(transacciones + " transacciones");

        if (cajaActual != null) {
            lblEstadoCaja.setText("ABIERTA");
            lblEstadoCaja.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
            lblFechaCaja.setText("Apertura: " + FormatUtil.formatearPrecio(cajaActual.getApertura()));
            lblMontApertura.setText(FormatUtil.formatearPrecio(cajaActual.getApertura()));
            lblDiferencia.setText("—");

            panelApertura.setVisible(false); panelApertura.setManaged(false);
            panelCierre.setVisible(true);    panelCierre.setManaged(true);
            lblVentasCierre.setText(FormatUtil.formatearPrecio(ventasDia));
        } else {
            lblEstadoCaja.setText("CERRADA");
            lblEstadoCaja.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
            lblFechaCaja.setText("Sin apertura hoy");
            lblMontApertura.setText("$0");
            lblDiferencia.setText("—");
            lblDiferenciaSub.setText("Cierra caja primero");

            panelApertura.setVisible(true);  panelApertura.setManaged(true);
            panelCierre.setVisible(false);   panelCierre.setManaged(false);
        }

        cargarHistorial();
    }

    @FXML private void abrirCaja() {
        BigDecimal monto = parseMonto(txtMontoApertura.getText());
        if (monto == null) {
            alerta("Ingresa un monto válido (número mayor o igual a 0)."); return;
        }
        int usuarioId = SessionManager.getInstance().getUsuarioActual().getId();
        cajaDAO.abrirCaja(monto, usuarioId);
        txtMontoApertura.clear();
        recargar();
    }

    @FXML private void cerrarCaja() {
        if (cajaActual == null) return;
        BigDecimal monto = parseMonto(txtMontoCierre.getText());
        if (monto == null) {
            alerta("Ingresa el monto de efectivo contado en caja."); return;
        }
        String obs = txtObservacion.getText().trim();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cerrar caja");
        confirm.setHeaderText("¿Confirmar cierre de caja?");
        BigDecimal apertura = cajaActual.getApertura();
        BigDecimal ventasDia = cajaDAO.totalVentasHoy();
        BigDecimal esperado = apertura.add(ventasDia);
        BigDecimal dif = monto.subtract(esperado);
        confirm.setContentText("Apertura: " + FormatUtil.formatearPrecio(apertura)
                + "\nVentas del día: " + FormatUtil.formatearPrecio(ventasDia)
                + "\nEsperado en caja: " + FormatUtil.formatearPrecio(esperado)
                + "\nEfectivo contado: " + FormatUtil.formatearPrecio(monto)
                + "\nDiferencia: " + FormatUtil.formatearPrecio(dif));

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            cajaDAO.cerrarCaja(cajaActual.getId(), monto, obs);
            txtMontoCierre.clear();
            txtObservacion.clear();
            recargar();
        });
    }

    @FXML private void calcularDiferencia() {
        BigDecimal monto = parseMonto(txtMontoCierre.getText());
        BigDecimal apertura = cajaActual != null ? cajaActual.getApertura() : BigDecimal.ZERO;
        BigDecimal esperado = apertura.add(cajaDAO.totalVentasHoy());
        if (monto != null) {
            BigDecimal dif = monto.subtract(esperado);
            lblDiferenciaPreview.setText(FormatUtil.formatearPrecio(dif));
            if (dif.compareTo(BigDecimal.ZERO) >= 0) {
                lblDiferenciaPreview.setStyle("-fx-font-weight: bold; -fx-text-fill: #22c55e;");
            } else {
                lblDiferenciaPreview.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444;");
            }
        } else {
            lblDiferenciaPreview.setText("—");
            lblDiferenciaPreview.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b;");
        }
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha()));
        colApertura.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.formatearPrecio(c.getValue().getApertura())));
        colVentas.setCellValueFactory(c -> new SimpleStringProperty(FormatUtil.formatearPrecio(c.getValue().getVentasDia())));
        colCierre.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isAbierta() ? "—" : FormatUtil.formatearPrecio(c.getValue().getCierre())));
        colDiferencia.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().isAbierta() ? "—" : FormatUtil.formatearPrecio(c.getValue().getDiferencia())));
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isAbierta() ? "Abierta" : "Cerrada"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label badge = new Label(s);
                badge.getStyleClass().add("Abierta".equals(s) ? "badge-activo" : "badge-inactivo");
                setGraphic(badge); setText(null);
            }
        });
        colUsuario.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUsuarioNombre() != null ? c.getValue().getUsuarioNombre() : "—"));
        tablaHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void cargarHistorial() {
        List<Caja> historial = cajaDAO.listarHistorial(30);
        tablaHistorial.setItems(FXCollections.observableArrayList(historial));

        if (cajaActual != null) {
            BigDecimal ventasDia = cajaDAO.totalVentasHoy();
            BigDecimal esperado = cajaActual.getApertura().add(ventasDia);
            lblDiferencia.setText(FormatUtil.formatearPrecio(esperado));
            lblDiferenciaSub.setText("Apertura + ventas del día");
        }
    }

    private BigDecimal parseMonto(String texto) {
        try {
            String limpio = texto.trim().replace(",", ".").replace("$", "");
            BigDecimal val = new BigDecimal(limpio);
            return val.compareTo(BigDecimal.ZERO) >= 0 ? val.setScale(0, RoundingMode.HALF_UP) : null;
        } catch (Exception e) { return null; }
    }

    private void alerta(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}

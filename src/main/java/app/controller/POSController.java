package app.controller;

import app.dao.CajaDAO;
import app.dao.ClienteDAO;
import app.dao.ProductoDAO;
import app.dao.VentaDAO;
import app.model.Cliente;
import app.model.DetalleVenta;
import app.model.Producto;
import app.model.Venta;
import app.util.FormatUtil;
import app.util.ReciboPDF;
import app.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class POSController {

    @FXML private HBox rootPane;
    @FXML private Label lblHeaderInfo;
    @FXML private Label lblClienteSeleccionado;
    @FXML private TextField txtBuscar;
    @FXML private FlowPane gridProductos;
    @FXML private VBox listaCarrito;
    @FXML private VBox carritoVacio;
    @FXML private ScrollPane scrollCarrito;
    @FXML private Label lblSubtotal;
    @FXML private TextField txtDescuento;
    @FXML private Label lblIva;
    @FXML private Label lblTotal;
    @FXML private Button btnCobrar;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final VentaDAO    ventaDAO    = new VentaDAO();
    private final ClienteDAO  clienteDAO  = new ClienteDAO();
    private final CajaDAO     cajaDAO     = new CajaDAO();

    private Integer clienteSeleccionadoId = null;

    // mapa productoId -> cantidad en carrito
    private final Map<Integer, Integer> carrito = new LinkedHashMap<>();
    private List<Producto> todosLosProductos = new ArrayList<>();
    private List<Producto> productosMostrados = new ArrayList<>();

    @FXML
    public void initialize() {
        var usuario = SessionManager.getInstance().getUsuarioActual();
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMM", new Locale("es")));
        lblHeaderInfo.setText((usuario != null ? usuario.getIniciales() : "")
                + "  " + (usuario != null ? usuario.getNombre() : "") + " · Hoy " + fecha);

        cargarProductos();
    }

    private void cargarProductos() {
        todosLosProductos = productoDAO.listarTodos();
        productosMostrados = new ArrayList<>(todosLosProductos);
        renderizarGrid(productosMostrados);
    }

    @FXML private void onBuscar() {
        String texto = txtBuscar.getText().toLowerCase().trim();
        if (texto.isEmpty()) {
            productosMostrados = new ArrayList<>(todosLosProductos);
        } else {
            productosMostrados = todosLosProductos.stream()
                    .filter(p -> p.getNombre().toLowerCase().contains(texto)
                            || p.getCodigo().toLowerCase().contains(texto))
                    .collect(java.util.stream.Collectors.toList());
        }
        renderizarGrid(productosMostrados);
    }

    private void renderizarGrid(List<Producto> productos) {
        gridProductos.getChildren().clear();
        for (Producto p : productos) {
            VBox card = crearCardProducto(p);
            gridProductos.getChildren().add(card);
        }
    }

    private VBox crearCardProducto(Producto p) {
        VBox card = new VBox(6);
        card.getStyleClass().add("producto-card");
        card.setPrefWidth(198);
        card.setMinWidth(170);

        // Chip categoría
        Label chip = new Label(p.getCategoria() != null ? p.getCategoria() : "");
        chip.getStyleClass().add("categoria-chip");

        Label nombre = new Label(p.getNombre());
        nombre.getStyleClass().add("producto-card-nombre");
        nombre.setWrapText(true);
        nombre.setMaxWidth(185);

        Label precio = new Label(FormatUtil.formatearPrecio(p.getPrecioVenta()));
        precio.getStyleClass().add("producto-card-precio");

        int enCarrito = carrito.getOrDefault(p.getId(), 0);
        int stockDisp = p.getStockActual() - enCarrito;

        Label stock = new Label("Stock: " + stockDisp);
        stock.getStyleClass().add("producto-card-stock");
        if (stockDisp <= 0) stock.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px;");

        card.getChildren().addAll(chip, nombre, precio, stock);

        if (stockDisp > 0) {
            card.setOnMouseClicked(e -> agregarAlCarrito(p));
        } else {
            card.setStyle("-fx-opacity: 0.55; -fx-cursor: default;");
        }

        return card;
    }

    private void agregarAlCarrito(Producto p) {
        int enCarrito = carrito.getOrDefault(p.getId(), 0);
        if (enCarrito >= p.getStockActual()) return;
        carrito.put(p.getId(), enCarrito + 1);
        actualizarCarritoUI();
        renderizarGrid(productosMostrados); // refrescar disponibilidad
    }

    private void actualizarCarritoUI() {
        listaCarrito.getChildren().clear();
        boolean vacio = carrito.isEmpty();
        carritoVacio.setVisible(vacio);
        carritoVacio.setManaged(vacio);
        scrollCarrito.setVisible(!vacio);
        scrollCarrito.setManaged(!vacio);
        btnCobrar.setDisable(vacio);

        for (Map.Entry<Integer, Integer> entry : carrito.entrySet()) {
            Producto p = buscarProductoPorId(entry.getKey());
            if (p == null) continue;
            int qty = entry.getValue();

            HBox fila = new HBox(8);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.setPadding(new javafx.geometry.Insets(0, 0, 8, 0));
            fila.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label nombre = new Label(p.getNombre());
            nombre.getStyleClass().add("carrito-item-nombre");
            nombre.setWrapText(true);
            nombre.setMaxWidth(150);

            Label precioUnit = new Label(FormatUtil.formatearPrecio(p.getPrecioVenta()) + " c/u");
            precioUnit.getStyleClass().add("carrito-item-precio");
            info.getChildren().addAll(nombre, precioUnit);

            // Controles cantidad
            Button menos = new Button("-");
            menos.getStyleClass().add("qty-btn");
            Label qtyLabel = new Label(String.valueOf(qty));
            qtyLabel.getStyleClass().add("qty-label");
            Button mas = new Button("+");
            mas.getStyleClass().add("qty-btn");

            HBox ctrlQty = new HBox(4, menos, qtyLabel, mas);
            ctrlQty.setAlignment(Pos.CENTER);

            // Precio total del item
            BigDecimal subtotalItem = p.getPrecioVenta().multiply(BigDecimal.valueOf(qty));
            Label precioTotal = new Label(FormatUtil.formatearPrecio(subtotalItem));
            precioTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");

            // Botón eliminar
            Button eliminar = new Button("✕");
            eliminar.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 0;");

            fila.getChildren().addAll(info, ctrlQty, precioTotal, eliminar);
            listaCarrito.getChildren().add(fila);

            final int pid = p.getId();
            menos.setOnAction(e -> {
                int actual = carrito.getOrDefault(pid, 0);
                if (actual <= 1) carrito.remove(pid);
                else carrito.put(pid, actual - 1);
                actualizarCarritoUI();
                renderizarGrid(productosMostrados);
            });
            mas.setOnAction(e -> {
                int actual = carrito.getOrDefault(pid, 0);
                if (actual < p.getStockActual()) carrito.put(pid, actual + 1);
                actualizarCarritoUI();
                renderizarGrid(productosMostrados);
            });
            eliminar.setOnAction(e -> {
                carrito.remove(pid);
                actualizarCarritoUI();
                renderizarGrid(productosMostrados);
            });
        }
        recalcular();
    }

    @FXML void recalcular() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> entry : carrito.entrySet()) {
            Producto p = buscarProductoPorId(entry.getKey());
            if (p == null) continue;
            subtotal = subtotal.add(p.getPrecioVenta().multiply(BigDecimal.valueOf(entry.getValue())));
        }

        double descPct = 0;
        try { descPct = Double.parseDouble(txtDescuento.getText().trim()); } catch (Exception ignored) {}
        if (descPct < 0) descPct = 0;
        if (descPct > 100) descPct = 100;

        BigDecimal descuento = subtotal.multiply(BigDecimal.valueOf(descPct / 100)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal baseIva = subtotal.subtract(descuento);
        BigDecimal iva = baseIva.multiply(BigDecimal.valueOf(0.19)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = baseIva.add(iva);

        lblSubtotal.setText(FormatUtil.formatearPrecio(subtotal));
        lblIva.setText(FormatUtil.formatearPrecio(iva));
        lblTotal.setText(FormatUtil.formatearPrecio(total));
    }

    @FXML private void seleccionarCliente() {
        List<Cliente> activos = clienteDAO.listarActivos();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(lblClienteSeleccionado.getScene().getWindow());
        dialog.setTitle("Seleccionar cliente");
        dialog.setResizable(false);

        VBox root = new VBox(12);
        root.setStyle("-fx-background-color: white; -fx-padding: 20;");
        root.setPrefWidth(380);

        Label titulo = new Label("Seleccionar cliente");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Buscar por nombre o cédula...");
        txtSearch.getStyleClass().add("text-field-custom");
        txtSearch.setPrefHeight(38);

        ListView<Cliente> lista = new ListView<>();
        lista.setPrefHeight(240);
        lista.setItems(FXCollections.observableArrayList(activos));
        lista.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) { setText(null); return; }
                String t = c.getNombre();
                if (c.getCedula() != null && !c.getCedula().isEmpty()) t += "  ·  " + c.getCedula();
                setText(t);
            }
        });

        txtSearch.setOnKeyReleased(e -> {
            String q = txtSearch.getText().trim();
            List<Cliente> filtrados = q.isEmpty() ? activos : clienteDAO.buscar(q)
                    .stream().filter(Cliente::isActivo).toList();
            lista.setItems(FXCollections.observableArrayList(filtrados));
        });

        Button btnConsumidor = new Button("Consumidor final");
        btnConsumidor.getStyleClass().add("btn-secondary");
        btnConsumidor.setPrefHeight(36);

        root.getChildren().addAll(titulo, txtSearch, lista, btnConsumidor);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        dialog.setScene(scene);

        lista.setOnMouseClicked(e -> {
            Cliente sel = lista.getSelectionModel().getSelectedItem();
            if (sel != null) {
                clienteSeleccionadoId = sel.getId();
                lblClienteSeleccionado.setText(sel.getNombre()
                        + (sel.getCedula() != null && !sel.getCedula().isEmpty() ? "  ·  " + sel.getCedula() : ""));
                lblClienteSeleccionado.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e293b; -fx-font-weight: bold;");
                dialog.close();
            }
        });

        btnConsumidor.setOnAction(e -> {
            clienteSeleccionadoId = null;
            lblClienteSeleccionado.setText("Consumidor final");
            lblClienteSeleccionado.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            dialog.close();
        });

        dialog.showAndWait();
    }

    @FXML private void limpiarCarrito() {
        carrito.clear();
        clienteSeleccionadoId = null;
        lblClienteSeleccionado.setText("Consumidor final");
        lblClienteSeleccionado.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        txtDescuento.setText("0");
        actualizarCarritoUI();
        renderizarGrid(productosMostrados);
    }

    @FXML private void cobrar() {
        if (carrito.isEmpty()) return;

        // No se puede vender sin una caja abierta del día
        if (cajaDAO.obtenerCajaHoy() == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Caja cerrada");
            a.setHeaderText("No hay una caja abierta");
            a.setContentText("Debes abrir la caja antes de registrar ventas.\n"
                    + "Ve al módulo «Caja» y realiza la apertura del día.");
            a.showAndWait();
            return;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<DetalleVenta> detalles = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : carrito.entrySet()) {
            Producto p = buscarProductoPorId(entry.getKey());
            if (p == null) continue;
            int qty = entry.getValue();
            subtotal = subtotal.add(p.getPrecioVenta().multiply(BigDecimal.valueOf(qty)));
            detalles.add(new DetalleVenta(p.getId(), p.getNombre(), qty, p.getPrecioVenta()));
        }

        double descPct = 0;
        try { descPct = Double.parseDouble(txtDescuento.getText().trim()); } catch (Exception ignored) {}
        BigDecimal descuento = subtotal.multiply(BigDecimal.valueOf(descPct / 100)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal baseIva = subtotal.subtract(descuento);
        BigDecimal iva = baseIva.multiply(BigDecimal.valueOf(0.19)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal total = baseIva.add(iva);

        // Confirmar cobro
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar venta");
        confirm.setHeaderText("Total a cobrar: " + FormatUtil.formatearPrecio(total));
        confirm.setContentText("¿Confirmar la venta de " + carrito.values().stream().mapToInt(Integer::intValue).sum() + " unidades?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            Venta venta = new Venta();
            venta.setTotal(total);
            venta.setDescuento(descuento);
            venta.setIva(iva);
            venta.setUsuarioId(SessionManager.getInstance().getUsuarioActual().getId());
            if (clienteSeleccionadoId != null) venta.setClienteId(clienteSeleccionadoId);
            venta.setDetalles(detalles);

            try {
                int ventaId = ventaDAO.registrarVenta(venta);
                String clienteNombre = lblClienteSeleccionado.getText();
                String cajeroNombre = SessionManager.getInstance().getUsuarioActual().getNombre();
                venta.setFecha(java.time.LocalDateTime.now());
                limpiarCarrito();
                cargarProductos();

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Venta registrada");
                ok.setHeaderText("✓ Venta #" + ventaId + " registrada exitosamente");
                ok.setContentText("Total cobrado: " + FormatUtil.formatearPrecio(total));
                ButtonType btnRecibo = new ButtonType("Recibo PDF", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnCerrar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
                ok.getButtonTypes().setAll(btnRecibo, btnCerrar);
                ok.showAndWait().ifPresent(b -> {
                    if (b == btnRecibo) generarYAbrirRecibo(venta, clienteNombre, cajeroNombre);
                });
            } catch (Exception ex) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Error");
                err.setHeaderText("No se pudo registrar la venta");
                err.setContentText(ex.getMessage());
                err.showAndWait();
            }
        });
    }

    private Producto buscarProductoPorId(int id) {
        return todosLosProductos.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
    }

    private void generarYAbrirRecibo(Venta venta, String clienteNombre, String cajeroNombre) {
        try {
            java.io.File dir = new java.io.File("recibos");
            if (!dir.exists()) dir.mkdirs();
            String stamp = java.time.LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            java.io.File destino = new java.io.File(dir, "recibo_" + venta.getId() + "_" + stamp + ".pdf");

            ReciboPDF.generar(venta, clienteNombre, cajeroNombre, destino);
            abrirArchivo(destino);
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo generar el recibo: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void abrirArchivo(java.io.File archivo) {
        if (java.awt.Desktop.isDesktopSupported()) {
            new Thread(() -> {
                try { java.awt.Desktop.getDesktop().open(archivo); } catch (Exception ignored) {}
            }).start();
        }
    }
}

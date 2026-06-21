package app.model;

import java.time.LocalDateTime;

public class Movimiento {
    private int id;
    private int productoId;
    private String productoNombre;
    private Tipo tipo;
    private int cantidad;
    private int stockResultante;
    private LocalDateTime fecha;
    private int usuarioId;
    private String usuarioNombre;
    private String observacion;

    public enum Tipo {
        entrada, salida, ajuste
    }

    public Movimiento() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }

    public String getProductoNombre() { return productoNombre; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getStockResultante() { return stockResultante; }
    public void setStockResultante(int stockResultante) { this.stockResultante = stockResultante; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getCantidadFormateada() {
        if (tipo == Tipo.salida) return "- " + cantidad;
        return "+ " + cantidad;
    }
}

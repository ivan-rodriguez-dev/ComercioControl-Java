package app.model;

import java.math.BigDecimal;

public class Producto {
    private int id;
    private String codigo;
    private String nombre;
    private String categoria;
    private BigDecimal precioCosto;
    private BigDecimal precioVenta;
    private int stockActual;
    private int stockMinimo;
    private Integer proveedorId;

    public enum Estado {
        Critico, Normal, Exceso
    }

    public Producto() {}

    public Producto(int id, String codigo, String nombre, String categoria,
                    BigDecimal precioCosto, BigDecimal precioVenta,
                    int stockActual, int stockMinimo, Integer proveedorId) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioCosto = precioCosto;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.proveedorId = proveedorId;
    }

    public Estado getEstado() {
        if (stockActual <= stockMinimo) return Estado.Critico;
        if (stockActual > stockMinimo * 4) return Estado.Exceso;
        return Estado.Normal;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public BigDecimal getPrecioCosto() { return precioCosto; }
    public void setPrecioCosto(BigDecimal precioCosto) { this.precioCosto = precioCosto; }

    public BigDecimal getPrecioVenta() { return precioVenta; }
    public void setPrecioVenta(BigDecimal precioVenta) { this.precioVenta = precioVenta; }

    public int getStockActual() { return stockActual; }
    public void setStockActual(int stockActual) { this.stockActual = stockActual; }

    public int getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(int stockMinimo) { this.stockMinimo = stockMinimo; }

    public Integer getProveedorId() { return proveedorId; }
    public void setProveedorId(Integer proveedorId) { this.proveedorId = proveedorId; }

    @Override
    public String toString() {
        return nombre + " (Stock: " + stockActual + ")";
    }
}

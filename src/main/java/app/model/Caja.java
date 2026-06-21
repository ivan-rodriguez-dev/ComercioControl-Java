package app.model;

import java.math.BigDecimal;

public class Caja {
    private int id;
    private String fecha;
    private BigDecimal apertura;
    private BigDecimal cierre;
    private BigDecimal ventasDia;
    private BigDecimal diferencia;
    private String observacion;
    private int usuarioId;
    private String usuarioNombre;
    private String estado;

    public Caja() {
        apertura = BigDecimal.ZERO;
        cierre = BigDecimal.ZERO;
        ventasDia = BigDecimal.ZERO;
        diferencia = BigDecimal.ZERO;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public BigDecimal getApertura() { return apertura; }
    public void setApertura(BigDecimal apertura) { this.apertura = apertura; }

    public BigDecimal getCierre() { return cierre; }
    public void setCierre(BigDecimal cierre) { this.cierre = cierre; }

    public BigDecimal getVentasDia() { return ventasDia; }
    public void setVentasDia(BigDecimal ventasDia) { this.ventasDia = ventasDia; }

    public BigDecimal getDiferencia() { return diferencia; }
    public void setDiferencia(BigDecimal diferencia) { this.diferencia = diferencia; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public boolean isAbierta() { return "abierta".equals(estado); }
}

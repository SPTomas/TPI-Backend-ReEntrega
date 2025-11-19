package com.gestiontransporte.logistica.dto;

import java.math.BigDecimal;

public class PuntoRutaDTO {

    private BigDecimal latitud;
    private BigDecimal longitud;

    // Si el punto corresponde a un depósito
    private Long idDeposito;

    public BigDecimal getLatitud() { return latitud; }
    public void setLatitud(BigDecimal latitud) { this.latitud = latitud; }

    public BigDecimal getLongitud() { return longitud; }
    public void setLongitud(BigDecimal longitud) { this.longitud = longitud; }

    public Long getIdDeposito() { return idDeposito; }
    public void setIdDeposito(Long idDeposito) { this.idDeposito = idDeposito; }

    @Override
    public String toString() {
        return "PuntoRutaDTO{" +
                "latitud=" + latitud +
                ", longitud=" + longitud +
                ", idDeposito=" + idDeposito +
                '}';
    }
}

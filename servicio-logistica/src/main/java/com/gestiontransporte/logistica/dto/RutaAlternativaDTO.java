package com.gestiontransporte.logistica.dto;

import java.math.BigDecimal;
import java.util.List;

public class RutaAlternativaDTO {

    private String nombre;               // "Ruta directa", "Depósitos [1]", etc.
    private BigDecimal costoEstimado;
    private BigDecimal tiempoEstimadoMin;
    private int cantidadTramos;
    private int cantidadDepositos;
    private List<String> descripcionTramos;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getCostoEstimado() {
        return costoEstimado;
    }

    public void setCostoEstimado(BigDecimal costoEstimado) {
        this.costoEstimado = costoEstimado;
    }

    public BigDecimal getTiempoEstimadoMin() {
        return tiempoEstimadoMin;
    }

    public void setTiempoEstimadoMin(BigDecimal tiempoEstimadoMin) {
        this.tiempoEstimadoMin = tiempoEstimadoMin;
    }

    public int getCantidadTramos() {
        return cantidadTramos;
    }

    public void setCantidadTramos(int cantidadTramos) {
        this.cantidadTramos = cantidadTramos;
    }

    public int getCantidadDepositos() {
        return cantidadDepositos;
    }

    public void setCantidadDepositos(int cantidadDepositos) {
        this.cantidadDepositos = cantidadDepositos;
    }

    public List<String> getDescripcionTramos() {
        return descripcionTramos;
    }

    public void setDescripcionTramos(List<String> descripcionTramos) {
        this.descripcionTramos = descripcionTramos;
    }
}

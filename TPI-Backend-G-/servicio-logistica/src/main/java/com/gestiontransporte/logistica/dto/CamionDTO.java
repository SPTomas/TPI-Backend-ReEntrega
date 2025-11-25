package com.gestiontransporte.logistica.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CamionDTO {
    private String patente;
    private BigDecimal capacidadPeso;
    private BigDecimal capacidadVolumen;
    private Boolean disponibilidad;
    private BigDecimal consumoRealLitrosKm;
}
package com.gestiontransporte.logistica.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TramoAlternativoDTO {

    private String tipo; 
    private String descripcionOrigen;
    private String descripcionDestino;

    private BigDecimal latitudOrigen;
    private BigDecimal longitudOrigen;

    private BigDecimal latitudDestino;
    private BigDecimal longitudDestino;

    private BigDecimal distanciaKm;
    private BigDecimal tiempoMin;
}

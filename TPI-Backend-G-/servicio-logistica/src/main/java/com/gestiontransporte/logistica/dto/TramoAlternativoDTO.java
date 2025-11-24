package com.gestiontransporte.logistica.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TramoAlternativoDTO {

    private String tipo; // origen-destino, origen-deposito, deposito-deposito, deposito-destino

    private String descripcionOrigen;
    private String descripcionDestino;

    private BigDecimal latitudOrigen;
    private BigDecimal longitudOrigen;

    private BigDecimal latitudDestino;
    private BigDecimal longitudDestino;

    private BigDecimal distanciaKm;
    private BigDecimal tiempoMin;
}

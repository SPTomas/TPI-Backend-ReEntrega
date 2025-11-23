package com.gestiontransporte.logistica.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SolicitudDTO {
    private Long numero;
    private Long idContenedor;
    private Long idCliente;
    private BigDecimal costoEstimado;
    private BigDecimal costoFinal;
    private Long tiempoEstimado;
    private Long tiempoReal;
    private String estado;
}


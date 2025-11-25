package com.gestiontransporte.logistica.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinalizarSolicitudDTO {

    private BigDecimal costoFinal;
    private Long tiempoReal; 
    private String estado;    
}

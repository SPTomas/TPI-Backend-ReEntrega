package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FinalizarSolicitudDTO {

    private BigDecimal costoFinal;
    private Long tiempoReal;  
    private String estado;    
}

package com.gestiontransporte.logistica.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LocalizacionDTO {
    private Long idLocalizacion;   
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String descripcion;
}

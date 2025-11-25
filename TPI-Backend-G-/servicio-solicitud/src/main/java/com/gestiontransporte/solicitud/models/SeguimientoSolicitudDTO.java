package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SeguimientoSolicitudDTO {

    private Long numeroSolicitud;
    private String estadoSolicitud;
    private BigDecimal costoEstimado;
    private BigDecimal costoFinal;
    private Long tiempoEstimado; 
    private Long tiempoReal;     

    private Integer cantidadTramos;
    private Integer cantidadDepositos;
    private String estadoRuta; 

    private TramoSeguimientoDTO tramoActual;
    private List<TramoSeguimientoDTO> tramos;
}

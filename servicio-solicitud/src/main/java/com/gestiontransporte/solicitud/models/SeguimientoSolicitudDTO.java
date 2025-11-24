package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SeguimientoSolicitudDTO {

    // --- Datos de la solicitud ---
    private Long numeroSolicitud;
    private String estadoSolicitud;
    private BigDecimal costoEstimado;
    private BigDecimal costoFinal;
    private Long tiempoEstimado; // minutos
    private Long tiempoReal;     // minutos

    // --- Datos de la ruta ---
    private Integer cantidadTramos;
    private Integer cantidadDepositos;
    private String estadoRuta; // NO_INICIADA / EN_CURSO / FINALIZADA

    private TramoSeguimientoDTO tramoActual;
    private List<TramoSeguimientoDTO> tramos;
}

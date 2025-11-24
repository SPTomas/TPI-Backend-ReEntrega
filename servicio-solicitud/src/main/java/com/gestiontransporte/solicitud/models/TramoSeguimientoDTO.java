package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TramoSeguimientoDTO {
    private Long idTramo;
    private String tipo;
    private String estado;

    private String depositoOrigen;   // nombre del depósito o null
    private String depositoDestino;  // nombre del depósito o null

    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;

    private String patenteCamion;
}

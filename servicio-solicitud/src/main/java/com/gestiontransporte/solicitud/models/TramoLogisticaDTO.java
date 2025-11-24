package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TramoLogisticaDTO {

    private Long idTramo;
    private String tipo;
    private String estado;

    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;

    private String patenteCamion;

    private DepositoLogisticaDTO depositoOrigen;
    private DepositoLogisticaDTO depositoDestino;
}

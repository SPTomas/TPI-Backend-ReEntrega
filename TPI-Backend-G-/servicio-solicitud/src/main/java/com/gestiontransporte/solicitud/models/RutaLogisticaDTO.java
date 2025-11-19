package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.util.List;

@Data
public class RutaLogisticaDTO {

    private Long idRuta;
    private Long idSolicitud;
    private Integer cantidadTramos;
    private Integer cantidadDepositos;
    private List<TramoLogisticaDTO> tramos;
}

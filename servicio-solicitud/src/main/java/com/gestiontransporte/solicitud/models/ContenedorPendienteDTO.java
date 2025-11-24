package com.gestiontransporte.solicitud.models;

import lombok.Data;

@Data
public class ContenedorPendienteDTO {

    private Long idContenedor;
    private Long idCliente;
    private String estadoContenedor;

    private Long numeroSolicitud;
    private String estadoSolicitud;

    // Descripción textual de la ubicación actual
    private String ubicacion;
}

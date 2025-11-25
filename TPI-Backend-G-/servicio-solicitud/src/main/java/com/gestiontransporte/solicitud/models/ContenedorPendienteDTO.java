package com.gestiontransporte.solicitud.models;

import lombok.Data;

@Data
public class ContenedorPendienteDTO {

    private Long idContenedor;
    private Long idCliente;
    private String estadoContenedor;

    private Long numeroSolicitud;
    private String estadoSolicitud;

    private String ubicacion;
}

package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.math.BigDecimal;


@Data
public class SolicitudRequestDTO {

    private Long idCliente;

    private String nombreCliente;
    private String emailCliente;
    private String telefonoCliente;

    private BigDecimal costoEstimado;
    private long tiempoEstimado;

    private BigDecimal pesoContenedor;
    private BigDecimal volumenContenedor;

    private LocalizacionDTO origen;
    private LocalizacionDTO destino;
}

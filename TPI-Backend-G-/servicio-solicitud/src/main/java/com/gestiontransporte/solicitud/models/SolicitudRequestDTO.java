package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para recibir el JSON de una nueva solicitud.
 * Contiene los datos de la Solicitud Y los datos del Contenedor a crear.
 */
@Data
public class SolicitudRequestDTO {

    // Si viene -> se usa ese cliente existente
    private Long idCliente;

    // Datos para registrar cliente si no existe
    private String nombreCliente;

    private String emailCliente;

    private String telefonoCliente;

    // Datos de la Solicitud
    private BigDecimal costoEstimado;
    private long tiempoEstimado;
    // (puedes agregar tiempoEstimado aquí también)

    // Datos del Contenedor a crear
    private BigDecimal pesoContenedor;
    private BigDecimal volumenContenedor;
}
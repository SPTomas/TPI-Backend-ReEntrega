package com.gestiontransporte.solicitud.models;

import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO para recibir el JSON de una nueva solicitud.
 * Contiene datos de Solicitud, Cliente y Contenedor.
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

    // Datos del Contenedor a crear
    private BigDecimal pesoContenedor;
    private BigDecimal volumenContenedor;

    // 🔹 NUEVO: Origen y Destino
    private LocalizacionDTO origen;
    private LocalizacionDTO destino;
}

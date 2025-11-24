package com.gestiontransporte.logistica.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarEstadoTramoRequestDTO {

    /**
     * Valores esperados (ejemplo):
     *  - "SIN_EMPEZAR"
     *  - "EN_TRASLADO"  → cuando inicia el tramo
     *  - "FINALIZADO"   → cuando termina el tramo
     */
    @NotBlank
    private String nuevoEstado;
        // Opcional, pero lo usamos cuando el estado = "FINALIZADO"
    private BigDecimal costoReal;

}
package com.gestiontransporte.logistica.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarEstadoTramoRequestDTO {

    @NotBlank
    private String nuevoEstado;
    private BigDecimal costoReal;

}
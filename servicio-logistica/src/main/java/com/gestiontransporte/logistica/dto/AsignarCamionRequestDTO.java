package com.gestiontransporte.logistica.dto;

import lombok.Data;

// DTO para el body del request al asignar camion
@Data
public class AsignarCamionRequestDTO {
    private String patente;
}
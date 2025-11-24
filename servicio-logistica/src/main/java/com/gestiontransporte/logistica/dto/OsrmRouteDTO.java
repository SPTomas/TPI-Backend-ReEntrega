package com.gestiontransporte.logistica.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Ignora campos que no necesitamos
public class OsrmRouteDTO {
    private double distance; // En metros
    private double duration; // En segundos
}

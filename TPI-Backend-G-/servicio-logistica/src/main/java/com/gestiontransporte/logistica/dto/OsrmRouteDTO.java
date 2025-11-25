package com.gestiontransporte.logistica.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) 
public class OsrmRouteDTO {
    private double distance; 
    private double duration; 
}

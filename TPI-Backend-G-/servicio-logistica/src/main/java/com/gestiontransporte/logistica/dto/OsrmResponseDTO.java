package com.gestiontransporte.logistica.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) 
public class OsrmResponseDTO {
    private String code;
    private List<OsrmRouteDTO> routes;
}

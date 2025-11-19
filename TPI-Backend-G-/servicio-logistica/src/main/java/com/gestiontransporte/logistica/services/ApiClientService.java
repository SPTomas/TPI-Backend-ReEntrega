package com.gestiontransporte.logistica.services;

import com.gestiontransporte.logistica.dto.CamionDTO;
import com.gestiontransporte.logistica.dto.SolicitudDTO;
import com.gestiontransporte.logistica.dto.OsrmResponseDTO; // ¡Ahora existe!
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ApiClientService {

    private final RestClient restClientTransporte;
    private final RestClient restClientSolicitud;
    private final RestClient restClientOsrm; // <-- AÑADIDO

    public ApiClientService(
            @Value("${api.servicios.transporte}") String baseUrlTransporte,
            @Value("${api.servicios.solicitud}") String baseUrlSolicitud,
            @Value("${api.distancia.url}") String baseUrlOsrm) { // <-- AÑADIDO
        
        this.restClientTransporte = RestClient.builder().baseUrl(baseUrlTransporte).build();
        this.restClientSolicitud = RestClient.builder().baseUrl(baseUrlSolicitud).build();
        this.restClientOsrm = RestClient.builder().baseUrl(baseUrlOsrm).build(); // <-- AÑADIDO
    }

    // --- Métodos para ServicioTransporte ---
    
    public CamionDTO getCamion(String patente) {
        try {
            return restClientTransporte.get()
                .uri("/camiones/{patente}", patente)
                .retrieve()
                .body(CamionDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    // --- Métodos para ServicioSolicitud ---

    public SolicitudDTO getSolicitud(Long idSolicitud) {
         try {
            return restClientSolicitud.get()
                .uri("/{numero}", idSolicitud)
                .retrieve()
                .body(SolicitudDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    // --- ¡MÉTODO AÑADIDO PARA OSRM! ---

    /**
     * Llama al servicio OSRM para obtener distancia y duración.
     * @param origenLongLat (Ej: "-64.18105,-31.4135")
     * @param destinoLongLat (Ej: "-60.6985,-32.9471")
     * @return El DTO con la información de la ruta.
     */
    public OsrmResponseDTO getDistancia(String origenLongLat, String destinoLongLat) {
        
        String coords = origenLongLat + ";" + destinoLongLat;

        try {
            return restClientOsrm.get()
                .uri("/route/v1/driving/{coords}?overview=false", coords)
                .retrieve()
                .body(OsrmResponseDTO.class);
                
        } catch (Exception e) {
            System.err.println("Error al llamar a OSRM: " + e.getMessage());
            return null;
        }
    }
}
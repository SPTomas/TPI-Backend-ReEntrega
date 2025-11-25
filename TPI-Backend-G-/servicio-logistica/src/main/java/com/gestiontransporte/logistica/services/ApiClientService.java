package com.gestiontransporte.logistica.services;

import com.gestiontransporte.logistica.dto.CamionDTO;
import com.gestiontransporte.logistica.dto.SolicitudDTO;
import com.gestiontransporte.logistica.dto.OsrmResponseDTO;
import com.gestiontransporte.logistica.dto.PrecioGasoilDTO;
import com.gestiontransporte.logistica.dto.ContenedorDTO;
import com.gestiontransporte.logistica.dto.FinalizarSolicitudDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class ApiClientService {

    private final RestClient transporteClient;
    private final RestClient solicitudClient;
    private final RestClient osrmClient;
    private final RestClient contenedorClient;

    public ApiClientService(
            @Value("${api.servicios.transporte}") String baseUrlTransporte,
            @Value("${api.servicios.solicitud}") String baseUrlSolicitud,
            @Value("${api.distancia.url}") String baseUrlOsrm,
            @Value("${api.servicios.contenedor}") String baseUrlContenedor) {

        this.transporteClient = RestClient.builder().baseUrl(baseUrlTransporte).build();
        this.solicitudClient = RestClient.builder().baseUrl(baseUrlSolicitud).build();
        this.osrmClient = RestClient.builder().baseUrl(baseUrlOsrm).build();
        this.contenedorClient = RestClient.builder().baseUrl(baseUrlContenedor).build();
    }

    // --- Transporte ---

    public CamionDTO getCamion(String patente) {
        try {
            return transporteClient.get()
                    .uri("/camiones/{patente}", patente)
                    .retrieve()
                    .body(CamionDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void actualizarDisponibilidadCamion(String patente, boolean disponible) {
    try {
        Map<String, Object> body = new HashMap<>();
        body.put("disponible", disponible);

        transporteClient.patch()
                .uri("/camiones/{patente}/disponibilidad", patente)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    } catch (Exception e) {
        System.err.println("Error al actualizar disponibilidad del camión " 
                + patente + ": " + e.getMessage());
    }
}


    // --- Solicitud ---

    public SolicitudDTO getSolicitud(Long idSolicitud) {
        try {
            return solicitudClient.get()
                    .uri("/{numero}", idSolicitud)
                    .retrieve()
                    .body(SolicitudDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean existeSolicitud(Long idSolicitud) {
        return getSolicitud(idSolicitud) != null;
    }

    public void actualizarEstimacionSolicitud(Long idSolicitud,
                                              BigDecimal costoEstimado,
                                              Long tiempoEstimadoMin) {
        Map<String, Object> body = new HashMap<>();
        body.put("costoEstimado", costoEstimado);
        body.put("tiempoEstimado", tiempoEstimadoMin);

        try {
            solicitudClient.post()
                    .uri("/{id}/estimacion", idSolicitud)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("No se pudo actualizar estimación de solicitud "
                    + idSolicitud + ": " + e.getMessage());
        }
    }

    public void actualizarEstadoSolicitudEnTransito(Long idSolicitud) {
        Map<String, Object> body = new HashMap<>();
        body.put("estado", "EN_TRANSITO");

        try {
            solicitudClient.post()
                    .uri("/{id}/estado", idSolicitud)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("No se pudo marcar solicitud " + idSolicitud +
                    " como EN_TRANSITO: " + e.getMessage());
        }
    }

    public void finalizarSolicitud(Long idSolicitud, FinalizarSolicitudDTO dto) {
        try {
            solicitudClient.post()
                    .uri("/{id}/finalizar", idSolicitud)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("No se pudo finalizar la solicitud "
                    + idSolicitud + ": " + e.getMessage());
        }
    }

    // --- Contenedor ---

    public ContenedorDTO getContenedor(Long idContenedor) {
        try {
            return contenedorClient.get()
                    .uri("/{id}", idContenedor)
                    .retrieve()
                    .body(ContenedorDTO.class);
        } catch (Exception e) {
            return null;
        }
    }

    // --- OSRM ---

    public OsrmResponseDTO getDistancia(String coords) {
        try {
            return osrmClient.get()
                    .uri("/route/v1/driving/{coords}?overview=false", coords)
                    .retrieve()
                    .body(OsrmResponseDTO.class);
        } catch (Exception e) {
            System.err.println("Error al llamar a OSRM: " + e.getMessage());
            return null;
        }
    }


    public BigDecimal getPrecioGasoil() {
    try {
        PrecioGasoilDTO dto = transporteClient.get()
                .uri("/precios/gasoil")
                .retrieve()
                .body(PrecioGasoilDTO.class);

        return dto.getPrecioGasoil();
    } catch (Exception e) {
        throw new IllegalStateException("No se pudo obtener precio del gasoil");
    }
}

}

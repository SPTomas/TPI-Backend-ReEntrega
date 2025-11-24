package com.gestiontransporte.solicitud.controller;

import com.gestiontransporte.solicitud.models.FinalizarSolicitudDTO;
import com.gestiontransporte.solicitud.models.Solicitud;
import com.gestiontransporte.solicitud.models.SolicitudRequestDTO;
import com.gestiontransporte.solicitud.services.SolicitudService;
import jakarta.validation.Valid; // Para activar las validaciones
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gestiontransporte.solicitud.models.FinalizarSolicitudDTO;
import com.gestiontransporte.solicitud.models.SeguimientoSolicitudDTO;

import java.util.List;
import java.util.Optional;

/**
 * Controlador REST que expone los endpoints para gestionar Solicitudes.
 * Delega toda la lógica de negocio al SolicitudService.
 *
 * La ruta base para este controlador se define en 'application.properties'
 * con la propiedad 'server.servlet.context-path=/api/solicitud'.
 */
@RestController
// NO es necesario un @RequestMapping a nivel de clase si ya definiste el context-path
@RequestMapping("/solicitudes")

public class SolicitudController {

    // Inyección de dependencias del servicio (obligatorio)
    private final SolicitudService solicitudService;

    // Se recomienda la inyección por constructor
    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    /**
     * Endpoint para crear una nueva solicitud.
     * Responde a: POST http://localhost:8082/api/solicitud/
     */
    @PostMapping
    public ResponseEntity<Solicitud> crearSolicitud(@Valid @RequestBody SolicitudRequestDTO solicitudDTO) {
        
        Solicitud nuevaSolicitud = solicitudService.crearSolicitud(solicitudDTO);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaSolicitud);
    }
    /**
     * Endpoint para obtener todas las solicitudes.
     * Responde a: GET http://localhost:8082/api/solicitud/
     */
    @GetMapping
    public ResponseEntity<List<Solicitud>> obtenerTodasLasSolicitudes() {
        List<Solicitud> solicitudes = solicitudService.buscarTodasLasSolicitudes();
        
        // Devuelve 200 OK y la lista en el body [cite: 442]
        return ResponseEntity.ok(solicitudes);
    }

    /**
     * Endpoint para buscar una solicitud por su ID (numero).
     * Responde a: GET http://localhost:8082/api/solicitud/{numero}
     */
    @GetMapping("/{numero}")
    public ResponseEntity<Solicitud> obtenerSolicitudPorId(@PathVariable Long numero) {
        Optional<Solicitud> solicitud = solicitudService.buscarSolicitudPorId(numero);

        // Usa el patrón de Optional + ResponseEntity recomendado en el Apunte 17 [cite: 432-436]
        return solicitud.map(ResponseEntity::ok) // 200 OK si la encuentra
                      .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found si no
    }


    // ... (dentro de la clase SolicitudController)

    /**
     * Endpoint para REEMPLAZAR una solicitud (PUT).
     * Responde a: PUT http://localhost:8082/api/solicitud/{numero}
     */
    @PutMapping("/{numero}")
    public ResponseEntity<Solicitud> reemplazarSolicitud(@PathVariable Long numero, @Valid @RequestBody Solicitud solicitudNueva) {
        
        return solicitudService.reemplazarSolicitud(numero, solicitudNueva)
            .map(ResponseEntity::ok) // 200 OK
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    /**
     * Endpoint para ACTUALIZAR PARCIALMENTE una solicitud (PATCH).
     * Responde a: PATCH http://localhost:8082/api/solicitud/{numero}
     */
    @PatchMapping("/{numero}")
    public ResponseEntity<Solicitud> actualizarSolicitudParcial(
            @PathVariable Long numero, 
            @RequestBody java.util.Map<String, Object> updates) {
                
        return solicitudService.actualizarParcial(numero, updates)
            .map(ResponseEntity::ok) // 200 OK
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    // /**
    //  * Endpoint para BORRAR una solicitud (DELETE).
    //  * Responde a: DELETE http://localhost:8082/api/solicitud/{numero}
    //  */
    // @DeleteMapping("/{numero}")
    // public ResponseEntity<Void> borrarSolicitud(@PathVariable Long numero) {
    //     if (solicitudService.borrarSolicitud(numero)) {
    //         return ResponseEntity.noContent().build(); // 204 No Content (Éxito, sin respuesta)
    //     } else {
    //         return ResponseEntity.notFound().build(); // 404 Not Found
    //     }
    // }

    /**
     * Endpoint para CANCELAR una solicitud (Acción).
     * Responde a: POST http://localhost:8082/api/solicitud/{numero}/cancelar
     */
    @PostMapping("/{numero}/cancelar")
    public ResponseEntity<Solicitud> cancelarSolicitud(@PathVariable Long numero) {
        
        return solicitudService.cancelarSolicitud(numero)
            .map(ResponseEntity::ok) // 200 OK
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }


        @PostMapping("/{numero}/finalizar")
    public ResponseEntity<Solicitud> finalizarSolicitud(
            @PathVariable Long numero,
            @RequestBody FinalizarSolicitudDTO dto
    ) {
        return solicitudService.finalizarSolicitud(numero, dto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
        


    @PostMapping("/{numero}/estimacion")
public ResponseEntity<Solicitud> actualizarEstimacionSolicitud(
        @PathVariable Long numero,
        @RequestBody java.util.Map<String, Object> updates) {

    return solicitudService.actualizarParcial(numero, updates)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}

@PostMapping("/{numero}/estado")
public ResponseEntity<Solicitud> actualizarEstado(
        @PathVariable Long numero,
        @RequestBody java.util.Map<String, Object> body) {

    return solicitudService.actualizarParcial(numero, body)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}

@GetMapping("/{numero}/seguimiento")
public ResponseEntity<SeguimientoSolicitudDTO> obtenerSeguimiento(@PathVariable Long numero) {
    SeguimientoSolicitudDTO dto = solicitudService.obtenerSeguimiento(numero);
    if (dto == null) {
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(dto);
}

}
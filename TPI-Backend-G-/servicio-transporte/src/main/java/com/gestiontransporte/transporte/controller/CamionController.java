package com.gestiontransporte.transporte.controller;

import com.gestiontransporte.transporte.models.Camion;
import com.gestiontransporte.transporte.services.CamionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST que expone los endpoints para gestionar Camiones.
 * Todas las rutas están basadas en el Swagger
 * La ruta base es "/transporte/camiones".
 */
@RestController
@RequestMapping("/transporte/camiones") // Define la ruta base para todos los métodos
public class CamionController {

    private final CamionService camionService;

    public CamionController(CamionService camionService) {
        this.camionService = camionService;
    }

    /**
     * Endpoint para Listar camiones.
     * Responde a: GET /transporte/camiones
     */
    @GetMapping
    public ResponseEntity<List<Camion>> listarCamiones() {
        List<Camion> camiones = camionService.buscarTodos();
        return ResponseEntity.ok(camiones);
    }

    /**
     * Endpoint para Alta de camión.
     * Responde a: POST /transporte/camiones
     *
     * IMPORTANTE: El JSON que envíes debe tener un objeto 'transportista'
     * anidado que contenga al menos su 'idTransportista'.
     * Ej: {"patente": "AB123CD", "transportista": {"idTransportista": 1}, ...}
     */
    @PostMapping
    public ResponseEntity<Camion> altaCamion(@Valid @RequestBody Camion camion) {
        try {
            Camion nuevoCamion = camionService.altaCamion(camion);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoCamion);
        } catch (IllegalArgumentException e) {
            // Esto atrapa si el transportista no existe (del orElseThrow)
            // (En un futuro, esto lo haría el @ControllerAdvice)
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Endpoint para Obtener camión por patente.
     * Responde a: GET /transporte/camiones/{patente}
     */
    @GetMapping("/{patente}")
    public ResponseEntity<Camion> obtenerCamion(@PathVariable String patente) {
        
        return camionService.buscarPorPatente(patente)
            .map(ResponseEntity::ok) // 200 OK
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    /**
     * Endpoint para Reemplazar camión.
     * Responde a: PUT /transporte/camiones/{patente}
     */
    @PutMapping("/{patente}")
    public ResponseEntity<Camion> reemplazarCamion(
            @PathVariable String patente, 
            @Valid @RequestBody Camion camionNuevo) {
        
        try {
            return camionService.reemplazarCamion(patente, camionNuevo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // Transportista no encontrado
        }
    }

    /**
     * Endpoint para Archivar camión (soft-delete).
     * Responde a: DELETE /transporte/camiones/{patente}
     */
    @DeleteMapping("/{patente}")
    public ResponseEntity<Void> archivarCamion(@PathVariable String patente) {
        
        boolean archivado = camionService.archivarCamion(patente);
        
        if (archivado) {
            return ResponseEntity.noContent().build(); // 204 No Content (Éxito)
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }

        @PatchMapping("/{patente}/disponibilidad")
    public ResponseEntity<Camion> actualizarDisponibilidad(
            @PathVariable String patente,
            @RequestBody Map<String, Boolean> body
    ) {
        Boolean disponible = body.get("disponible");

        if (disponible == null) {
            return ResponseEntity.badRequest().build();
        }

        Camion camionActualizado = camionService.cambiarDisponibilidad(patente, disponible);
        return ResponseEntity.ok(camionActualizado);
    }

}
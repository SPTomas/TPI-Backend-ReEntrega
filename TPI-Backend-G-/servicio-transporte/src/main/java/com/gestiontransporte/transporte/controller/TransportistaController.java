package com.gestiontransporte.transporte.controller;

import com.gestiontransporte.transporte.models.Transportista;
import com.gestiontransporte.transporte.services.TransportistaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controlador REST que expone los endpoints para gestionar Transportistas.
 * Todas las rutas están basadas en el Swagger
 * La ruta base es "/transporte/transportistas".
 */
@RestController
@RequestMapping("/transporte/transportistas") // Define la ruta base para todos los métodos
public class TransportistaController {

    private final TransportistaService transportistaService;

    public TransportistaController(TransportistaService transportistaService) {
        this.transportistaService = transportistaService;
    }

    /**
     * Endpoint para Listar transportistas.
     * Responde a: GET /transporte/transportistas
     */
    @GetMapping
    public ResponseEntity<List<Transportista>> listarTransportistas() {
        List<Transportista> transportistas = transportistaService.buscarTodos();
        return ResponseEntity.ok(transportistas);
    }

    /**
     * Endpoint para Crear transportista.
     * Responde a: POST /transporte/transportistas
     */
    @PostMapping
    public ResponseEntity<Transportista> crearTransportista(@Valid @RequestBody Transportista transportista) {
        Transportista nuevoTransportista = transportistaService.crearTransportista(transportista);
        // Devuelve 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTransportista);
    }

    /**
     * Endpoint para Obtener transportista por ID.
     * Responde a: GET /transporte/transportistas/{idTransportista}
     */
    @GetMapping("/{idTransportista}")
    public ResponseEntity<Transportista> obtenerTransportista(@PathVariable Long idTransportista) {
        
        // Usa el patrón recomendado del Apunte 17 [cite: 432-436]
        return transportistaService.buscarPorId(idTransportista)
            .map(ResponseEntity::ok) // 200 OK
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    /**
     * Endpoint para Reemplazar transportista.
     * Responde a: PUT /transporte/transportistas/{idTransportista}
     */
    @PutMapping("/{idTransportista}")
    public ResponseEntity<Transportista> reemplazarTransportista(
            @PathVariable Long idTransportista, 
            @Valid @RequestBody Transportista transportistaNuevo) {
        
        return transportistaService.reemplazarTransportista(idTransportista, transportistaNuevo)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Endpoint para Archivar transportista (soft-delete).
     * Responde a: DELETE /transporte/transportistas/{idTransportista}
     */
    @DeleteMapping("/{idTransportista}")
    public ResponseEntity<Void> archivarTransportista(@PathVariable Long idTransportista) {
        
        boolean archivado = transportistaService.archivarTransportista(idTransportista);
        
        if (archivado) {
            return ResponseEntity.noContent().build(); // 204 No Content (Éxito)
        } else {
            return ResponseEntity.notFound().build(); // 404 Not Found
        }
    }
}
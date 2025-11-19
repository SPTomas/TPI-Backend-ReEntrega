package com.gestiontransporte.solicitud.controller;

import com.gestiontransporte.solicitud.models.Contenedor;
import com.gestiontransporte.solicitud.models.EstadoContenedorDTO;
import com.gestiontransporte.solicitud.services.ContenedorService;
import com.gestiontransporte.solicitud.models.ContenedorPendienteDTO;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;


import com.gestiontransporte.solicitud.models.ContenedorPendienteDTO;
/**
 * Controlador REST para gestionar Contenedores.
 * Se enfoca en el seguimiento [cite: 21] y la consulta.
 */
@RestController
@RequestMapping("/contenedores")
public class ContenedorController {

    private final ContenedorService contenedorService;

    public ContenedorController(ContenedorService contenedorService) {
        this.contenedorService = contenedorService;
    }

    /**
     * Endpoint para Listar todos los contenedores.
     * Responde a: GET /contenedores
     */
    @GetMapping
    public ResponseEntity<List<Contenedor>> listarContenedores() {
        return ResponseEntity.ok(contenedorService.buscarTodos());
    }

    /**
     * Endpoint para Obtener un contenedor por su ID.
     * Responde a: GET /contenedores/{idContenedor}
     */
    @GetMapping("/{idContenedor}")
    public ResponseEntity<Contenedor> obtenerContenedor(@PathVariable Long idContenedor) {
        return contenedorService.buscarPorId(idContenedor)
            .map(ResponseEntity::ok) // 200 OK
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    /**
     * Endpoint para actualizar el estado de un contenedor (Seguimiento).
     * Responde a: PATCH /contenedores/{idContenedor}/estado
     * Body esperado: {"estado": "EN_VIAJE"}
     */
    @PatchMapping("/{idContenedor}/estado")
    public ResponseEntity<Contenedor> actualizarEstado(
            @PathVariable Long idContenedor,
            @RequestBody Map<String, String> body) {
        
        String nuevoEstado = body.get("estado");
        if (nuevoEstado == null || nuevoEstado.isEmpty()) {
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        }

        return contenedorService.actualizarEstado(idContenedor, nuevoEstado)
            .map(ResponseEntity::ok) // 200 OK
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    /**
     * Endpoint para consultar solo el estado de un contenedor.
     * GET /contenedores/{idContenedor}/estado
     * Respuesta ejemplo: {"idContenedor": 1, "estado": "EN_VIAJE"}
     */
    @GetMapping("/{idContenedor}/estado")
    public ResponseEntity<EstadoContenedorDTO> obtenerEstado(@PathVariable Long idContenedor) {

        return contenedorService.buscarPorId(idContenedor)
                .map(c -> {
                    EstadoContenedorDTO dto =
                            new EstadoContenedorDTO(c.getIdContenedor(), c.getEstado());
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

      @GetMapping("/pendientes")
    // @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")  // cuando enchufes Keycloak
    public ResponseEntity<List<ContenedorPendienteDTO>> listarPendientes(
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) String estado) {

        List<ContenedorPendienteDTO> lista = contenedorService.buscarPendientes(idCliente, estado);
        return ResponseEntity.ok(lista);
    }
    
}
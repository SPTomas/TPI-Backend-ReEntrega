package com.gestiontransporte.transporte.controller;

import com.gestiontransporte.transporte.models.Camion;
import com.gestiontransporte.transporte.services.CamionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transporte/camiones") 
public class CamionController {

    private final CamionService camionService;

    public CamionController(CamionService camionService) {
        this.camionService = camionService;
    }

    @GetMapping
    public ResponseEntity<List<Camion>> listarCamiones() {
        List<Camion> camiones = camionService.buscarTodos();
        return ResponseEntity.ok(camiones);
    }


    @PostMapping
    public ResponseEntity<Camion> altaCamion(@Valid @RequestBody Camion camion) {
        try {
            Camion nuevoCamion = camionService.altaCamion(camion);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevoCamion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{patente}")
    public ResponseEntity<Camion> obtenerCamion(@PathVariable String patente) {
        
        return camionService.buscarPorPatente(patente)
            .map(ResponseEntity::ok) // 200 OK
            .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    @PutMapping("/{patente}")
    public ResponseEntity<Camion> reemplazarCamion(
            @PathVariable String patente, 
            @Valid @RequestBody Camion camionNuevo) {
        
        try {
            return camionService.reemplazarCamion(patente, camionNuevo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); 
        }
    }

    @DeleteMapping("/{patente}")
    public ResponseEntity<Void> archivarCamion(@PathVariable String patente) {
        boolean archivado = camionService.archivarCamion(patente);
        if (archivado) {
            return ResponseEntity.noContent().build(); 
        } else {
            return ResponseEntity.notFound().build();
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
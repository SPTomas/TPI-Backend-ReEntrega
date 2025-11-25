package com.gestiontransporte.solicitud.controller;

import com.gestiontransporte.solicitud.models.FinalizarSolicitudDTO;
import com.gestiontransporte.solicitud.models.Solicitud;
import com.gestiontransporte.solicitud.models.SolicitudRequestDTO;
import com.gestiontransporte.solicitud.services.SolicitudService;
import jakarta.validation.Valid; 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gestiontransporte.solicitud.models.FinalizarSolicitudDTO;
import com.gestiontransporte.solicitud.models.SeguimientoSolicitudDTO;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/solicitudes")

public class SolicitudController {
    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    public ResponseEntity<Solicitud> crearSolicitud(@Valid @RequestBody SolicitudRequestDTO solicitudDTO) {
        
        Solicitud nuevaSolicitud = solicitudService.crearSolicitud(solicitudDTO);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaSolicitud);
    }

    @GetMapping
    public ResponseEntity<List<Solicitud>> obtenerTodasLasSolicitudes() {
        List<Solicitud> solicitudes = solicitudService.buscarTodasLasSolicitudes();
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/{numero}")
    public ResponseEntity<Solicitud> obtenerSolicitudPorId(@PathVariable Long numero) {
        Optional<Solicitud> solicitud = solicitudService.buscarSolicitudPorId(numero);
        return solicitud.map(ResponseEntity::ok) 
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{numero}")
    public ResponseEntity<Solicitud> reemplazarSolicitud(@PathVariable Long numero, @Valid @RequestBody Solicitud solicitudNueva) {
        
        return solicitudService.reemplazarSolicitud(numero, solicitudNueva)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build()); 
    }

    @PatchMapping("/{numero}")
    public ResponseEntity<Solicitud> actualizarSolicitudParcial(
            @PathVariable Long numero, 
            @RequestBody java.util.Map<String, Object> updates) {
                
        return solicitudService.actualizarParcial(numero, updates)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build()); 
    }

    @PostMapping("/{numero}/cancelar")
    public ResponseEntity<Solicitud> cancelarSolicitud(@PathVariable Long numero) {
        
        return solicitudService.cancelarSolicitud(numero)
            .map(ResponseEntity::ok) 
            .orElseGet(() -> ResponseEntity.notFound().build()); 
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
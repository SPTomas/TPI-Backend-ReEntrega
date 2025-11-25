package com.gestiontransporte.transporte.controller;

import com.gestiontransporte.transporte.models.Transportista;
import com.gestiontransporte.transporte.services.TransportistaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/transporte/transportistas") 
public class TransportistaController {

    private final TransportistaService transportistaService;

    public TransportistaController(TransportistaService transportistaService) {
        this.transportistaService = transportistaService;
    }

    @GetMapping
    public ResponseEntity<List<Transportista>> listarTransportistas() {
        List<Transportista> transportistas = transportistaService.buscarTodos();
        return ResponseEntity.ok(transportistas);
    }

    @PostMapping
    public ResponseEntity<Transportista> crearTransportista(@Valid @RequestBody Transportista transportista) {
        Transportista nuevoTransportista = transportistaService.crearTransportista(transportista);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoTransportista);
    }

    @GetMapping("/{idTransportista}")
    public ResponseEntity<Transportista> obtenerTransportista(@PathVariable Long idTransportista) {
        return transportistaService.buscarPorId(idTransportista)
            .map(ResponseEntity::ok) 
            .orElseGet(() -> ResponseEntity.notFound().build()); 
    }

    @PutMapping("/{idTransportista}")
    public ResponseEntity<Transportista> reemplazarTransportista(
            @PathVariable Long idTransportista, 
            @Valid @RequestBody Transportista transportistaNuevo) {
        
        return transportistaService.reemplazarTransportista(idTransportista, transportistaNuevo)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{idTransportista}")
    public ResponseEntity<Void> archivarTransportista(@PathVariable Long idTransportista) {
        boolean archivado = transportistaService.archivarTransportista(idTransportista); 
        if (archivado) {
            return ResponseEntity.noContent().build(); 
        } else {
            return ResponseEntity.notFound().build(); 
        }
    }
}
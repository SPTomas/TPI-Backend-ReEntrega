package com.gestiontransporte.transporte.controller;

import com.gestiontransporte.transporte.dto.PrecioGasoilDTO;
import com.gestiontransporte.transporte.services.PreciosService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/transporte/precios")
public class PreciosController {

    private final PreciosService preciosService;

    public PreciosController(PreciosService preciosService) {
        this.preciosService = preciosService;
    }

    // GET /precios/gasoil  -> devuelve el precio actual
    @GetMapping("/gasoil")
    public ResponseEntity<PrecioGasoilDTO> getPrecioGasoil() {
        PrecioGasoilDTO dto = preciosService.obtenerPrecioGasoil();
        return ResponseEntity.ok(dto);
    }

    // PUT /precios/gasoil  -> actualiza el precio
    @PutMapping("/gasoil")
    public ResponseEntity<PrecioGasoilDTO> updatePrecioGasoil(
            @RequestBody @Valid PrecioGasoilDTO request
    ) {
        if (request.getPrecioGasoil() == null ||
                request.getPrecioGasoil().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        PrecioGasoilDTO dto = preciosService.actualizarPrecioGasoil(request.getPrecioGasoil());
        return ResponseEntity.ok(dto);
    }
}

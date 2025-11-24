package com.gestiontransporte.transporte.services;

import com.gestiontransporte.transporte.dto.PrecioGasoilDTO;
import com.gestiontransporte.transporte.models.Precios;
import com.gestiontransporte.transporte.repositories.PreciosRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PreciosService {

    private final PreciosRepository preciosRepository;

    public PreciosService(PreciosRepository preciosRepository) {
        this.preciosRepository = preciosRepository;
    }

    // Siempre trabajamos con el registro id = 1
    private static final Long PRECIO_UNICO_ID = 1L;

    @Transactional(readOnly = true)
    public PrecioGasoilDTO obtenerPrecioGasoil() {
        Precios precios = preciosRepository.findById(PRECIO_UNICO_ID)
                .orElseThrow(() -> new RuntimeException("Precio de gasoil no configurado"));

        PrecioGasoilDTO dto = new PrecioGasoilDTO();
        dto.setPrecioGasoil(precios.getPrecioGasoil());
        return dto;
    }

    @Transactional
    public PrecioGasoilDTO actualizarPrecioGasoil(BigDecimal nuevoPrecio) {
        Precios precios = preciosRepository.findById(1L)
                .orElseGet(() -> {
                    Precios p = new Precios();
                    return p;
                });

        precios.setPrecioGasoil(nuevoPrecio);
        preciosRepository.save(precios);

        PrecioGasoilDTO dto = new PrecioGasoilDTO();
        dto.setPrecioGasoil(precios.getPrecioGasoil());
        return dto;
    }
}

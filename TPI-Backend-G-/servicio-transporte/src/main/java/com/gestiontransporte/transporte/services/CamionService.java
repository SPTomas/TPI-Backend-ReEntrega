package com.gestiontransporte.transporte.services;


import com.gestiontransporte.transporte.models.Transportista;
import com.gestiontransporte.transporte.models.Camion;
import com.gestiontransporte.transporte.repositories.CamionRepository;
import com.gestiontransporte.transporte.repositories.TransportistaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class CamionService {

    private final CamionRepository camionRepository;
    private final TransportistaRepository transportistaRepository; 

    public CamionService(CamionRepository camionRepository, TransportistaRepository transportistaRepository) {
        this.camionRepository = camionRepository;
        this.transportistaRepository = transportistaRepository;
    }

    public List<Camion> buscarTodos() {
        return camionRepository.findAll();
    }

    public Optional<Camion> buscarPorPatente(String patente) {
        return camionRepository.findById(patente);
    }

    public Camion altaCamion(Camion camion) {
        Long idTransportista = camion.getTransportista().getIdTransportista();
        Transportista transportista = transportistaRepository.findById(idTransportista)
                .orElseThrow(() -> new IllegalArgumentException("El Transportista con ID " + idTransportista + " no existe."));
        camion.setTransportista(transportista);
        camion.setEstado("ACTIVO");
        return camionRepository.save(camion);
    }

    public Optional<Camion> reemplazarCamion(String patente, Camion camionNuevo) {
        if (!camionRepository.existsById(patente)) {
            return Optional.empty(); 
        }
        Long idTransportista = camionNuevo.getTransportista().getIdTransportista();
        Transportista transportista = transportistaRepository.findById(idTransportista)
                .orElseThrow(() -> new IllegalArgumentException("El Transportista con ID " + idTransportista + " no existe."));
        camionNuevo.setPatente(patente); 
        camionNuevo.setTransportista(transportista); 
        
        return Optional.of(camionRepository.save(camionNuevo));
    }
    public boolean archivarCamion(String patente) {
        
        return camionRepository.findById(patente)
            .map(camion -> {
                camion.setEstado("ARCHIVADO"); 
                camionRepository.save(camion);
                return true;
            })
            .orElse(false); 
    }
    public Camion cambiarDisponibilidad(String patente, Boolean disponible) {
        Camion c = camionRepository.findById(patente)
            .orElseThrow(() -> new RuntimeException("No existe el camión " + patente));
        c.setDisponibilidad(disponible);
        return camionRepository.save(c);
    }
}
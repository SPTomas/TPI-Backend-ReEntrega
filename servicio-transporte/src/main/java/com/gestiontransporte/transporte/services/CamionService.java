package com.gestiontransporte.transporte.services;


import com.gestiontransporte.transporte.models.Transportista;
import com.gestiontransporte.transporte.models.Camion;
import com.gestiontransporte.transporte.repositories.CamionRepository;
import com.gestiontransporte.transporte.repositories.TransportistaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Capa de Servicio para la lógica de negocio de Camiones.
 * Se comunica con CamionRepository y TransportistaRepository.
 */
@Service
public class CamionService {

    private final CamionRepository camionRepository;
    private final TransportistaRepository transportistaRepository; // Para validar la FK

    // Inyectamos ambos repositorios
    public CamionService(CamionRepository camionRepository, TransportistaRepository transportistaRepository) {
        this.camionRepository = camionRepository;
        this.transportistaRepository = transportistaRepository;
    }

    /**
     * Obtiene todos los camiones.
     */
    public List<Camion> buscarTodos() {
        return camionRepository.findAll();
    }

    /**
     * Busca un camión por su patente (PK).
     */
    public Optional<Camion> buscarPorPatente(String patente) {
        return camionRepository.findById(patente);
    }

    /**
     * Da de alta un camión (POST).
     * Necesita el ID de un transportista existente.
     */
    public Camion altaCamion(Camion camion) {
        // 1. Validamos que el transportista exista
        Long idTransportista = camion.getTransportista().getIdTransportista();
        Transportista transportista = transportistaRepository.findById(idTransportista)
                .orElseThrow(() -> new IllegalArgumentException("El Transportista con ID " + idTransportista + " no existe."));

        // 2. Si existe, lo asignamos al camión
        camion.setTransportista(transportista);
        
        // 3. Asignamos estado "ACTIVO" por defecto
        camion.setEstado("ACTIVO");

        // 4. Guardamos el camión.
        // Como la patente es el ID, si ya existe, esto fallará (lo cual es bueno)
        return camionRepository.save(camion);
    }

    /**
     * Reemplaza un camión existente (PUT).
     */
    public Optional<Camion> reemplazarCamion(String patente, Camion camionNuevo) {
        
        // 1. Validamos que el camión a reemplazar exista
        if (!camionRepository.existsById(patente)) {
            return Optional.empty(); // No se encontró el camión, no podemos reemplazar
        }

        // 2. Validamos que el nuevo transportista exista
        Long idTransportista = camionNuevo.getTransportista().getIdTransportista();
        Transportista transportista = transportistaRepository.findById(idTransportista)
                .orElseThrow(() -> new IllegalArgumentException("El Transportista con ID " + idTransportista + " no existe."));
        
        // 3. Si todo es válido, asignamos la patente (PK) y guardamos.
        camionNuevo.setPatente(patente); // Aseguramos que la PK no cambie
        camionNuevo.setTransportista(transportista); // Asignamos la entidad completa
        
        return Optional.of(camionRepository.save(camionNuevo));
    }

    /**
     * Archiva un camión (Soft-Delete).
     * @return true si se archivó, false si no se encontró.
     */
    public boolean archivarCamion(String patente) {
        
        return camionRepository.findById(patente)
            .map(camion -> {
                camion.setEstado("ARCHIVADO"); // Lógica de Soft-Delete
                camionRepository.save(camion);
                return true;
            })
            .orElse(false); // No se encontró el camión
    }
}
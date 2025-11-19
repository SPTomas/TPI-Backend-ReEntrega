package com.gestiontransporte.transporte.services;

import com.gestiontransporte.transporte.models.Transportista;
import com.gestiontransporte.transporte.repositories.TransportistaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Capa de Servicio que centraliza la lógica de negocio para los Transportistas.
 * Implementa el "soft-delete" cambiando el estado a "ARCHIVADO".
 */
@Service
public class TransportistaService {

    private final TransportistaRepository transportistaRepository;

    public TransportistaService(TransportistaRepository transportistaRepository) {
        this.transportistaRepository = transportistaRepository;
    }

    /**
     * Obtiene todos los transportistas.
     * (Más adelante se puede filtrar para que solo traiga "ACTIVOS")
     */
    public List<Transportista> buscarTodos() {
        return transportistaRepository.findAll();
    }

    /**
     * Busca un transportista por su ID.
     */
    public Optional<Transportista> buscarPorId(Long idTransportista) {
        return transportistaRepository.findById(idTransportista);
    }

    /**
     * Crea un nuevo transportista.
     * Por defecto, lo crea con estado "ACTIVO".
     */
    public Transportista crearTransportista(Transportista transportista) {
        // Asigna estado "ACTIVO" por defecto al crear uno nuevo
        transportista.setEstado("ACTIVO");
        return transportistaRepository.save(transportista);
    }

    /**
     * Reemplaza por completo un transportista existente (PUT).
     */
    public Optional<Transportista> reemplazarTransportista(Long idTransportista, Transportista transportistaNuevo) {
        
        return transportistaRepository.findById(idTransportista)
            .map(transportistaActual -> {
                // Reemplaza todos los campos
                transportistaActual.setNombre(transportistaNuevo.getNombre());
                transportistaActual.setApellido(transportistaNuevo.getApellido());
                transportistaActual.setTelefono(transportistaNuevo.getTelefono());
                transportistaActual.setEstado(transportistaNuevo.getEstado()); // Mantiene el estado que le pases
                
                return transportistaRepository.save(transportistaActual);
            });
    }

    /**
     * Archiva un transportista (Soft-Delete).
     * En lugar de borrar, cambia el estado a "ARCHIVADO".
     * @return true si se archivó, false si no se encontró.
     */
    public boolean archivarTransportista(Long idTransportista) {
        
        Optional<Transportista> transportistaOpt = transportistaRepository.findById(idTransportista);
        
        if (transportistaOpt.isPresent()) {
            Transportista transportista = transportistaOpt.get();
            transportista.setEstado("ARCHIVADO"); // Lógica de Soft-Delete
            transportistaRepository.save(transportista);
            return true;
        }
        
        return false; // No se encontró el transportista
    }
}
package com.gestiontransporte.transporte.services;

import com.gestiontransporte.transporte.models.Transportista;
import com.gestiontransporte.transporte.repositories.TransportistaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class TransportistaService {

    private final TransportistaRepository transportistaRepository;

    public TransportistaService(TransportistaRepository transportistaRepository) {
        this.transportistaRepository = transportistaRepository;
    }

    public List<Transportista> buscarTodos() {
        return transportistaRepository.findAll();
    }

    public Optional<Transportista> buscarPorId(Long idTransportista) {
        return transportistaRepository.findById(idTransportista);
    }

    public Transportista crearTransportista(Transportista transportista) {
        transportista.setEstado("ACTIVO");
        return transportistaRepository.save(transportista);
    }

    public Optional<Transportista> reemplazarTransportista(Long idTransportista, Transportista transportistaNuevo) {
        
        return transportistaRepository.findById(idTransportista)
            .map(transportistaActual -> {
                transportistaActual.setNombre(transportistaNuevo.getNombre());
                transportistaActual.setApellido(transportistaNuevo.getApellido());
                transportistaActual.setTelefono(transportistaNuevo.getTelefono());
                transportistaActual.setEstado(transportistaNuevo.getEstado());
                
                return transportistaRepository.save(transportistaActual);
            });
    }

    public boolean archivarTransportista(Long idTransportista) {
        Optional<Transportista> transportistaOpt = transportistaRepository.findById(idTransportista);
        if (transportistaOpt.isPresent()) {
            Transportista transportista = transportistaOpt.get();
            transportista.setEstado("ARCHIVADO"); 
            transportistaRepository.save(transportista);
            return true;
        }
        return false; 
    }
}
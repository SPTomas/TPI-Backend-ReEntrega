package com.gestiontransporte.solicitud.services;

import com.gestiontransporte.solicitud.models.Cliente;
import com.gestiontransporte.solicitud.repositories.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Optional<Cliente> buscarPorId(Long idCliente) {
        return clienteRepository.findById(idCliente);
    }

    public Optional<Cliente> buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    public Cliente guardar(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    /**
     * Devuelve el cliente si ya existe por email, o lo crea.
     */
    public Cliente obtenerOCrearPorEmail(String email, String nombre, String telefono) {
        return clienteRepository.findByEmail(email)
                .orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setEmail(email);
                    nuevo.setNombre(nombre);
                    nuevo.setTelefono(telefono);
                    return clienteRepository.save(nuevo);
                });
    }
}

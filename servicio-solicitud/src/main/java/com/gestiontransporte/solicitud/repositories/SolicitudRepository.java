package com.gestiontransporte.solicitud.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestiontransporte.solicitud.models.Solicitud;

/**
 * Repositorio de Spring Data JPA para la entidad Solicitud.
 * Extiende JpaRepository para obtener métodos CRUD y de paginación
 * listos para usar[cite: 23, 212].
 */
@Repository // (Opcional, pero buena práctica) Indica que es un bean de repositorio
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    

        Optional<Solicitud> findByIdContenedor(Long idContenedor);
    // Spring Data JPA implementará automáticamente:
    // - save(Solicitud solicitud)
    // - findById(Long numero)
    // - findAll()
    // - deleteById(Long numero)
    // - ...y muchos más!

    // Aquí podrías agregar "consultas derivadas" si las necesitaras[cite: 22].
    // Por ejemplo, para buscar todas las solicitudes de un cliente:
    // List<Solicitud> findByIdCliente(Long idCliente);
}
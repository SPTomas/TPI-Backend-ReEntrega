package com.gestiontransporte.solicitud.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestiontransporte.solicitud.models.Solicitud;

/**
 * Repositorio de Spring Data JPA para la entidad Solicitud.
 * Extiende JpaRepository para obtener métodos CRUD y de paginación
 */
@Repository // (Opcional, pero buena práctica) Indica que es un bean de repositorio
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    

        Optional<Solicitud> findByIdContenedor(Long idContenedor);

;
}
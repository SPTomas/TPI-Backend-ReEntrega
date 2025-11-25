package com.gestiontransporte.solicitud.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestiontransporte.solicitud.models.Solicitud;


@Repository 
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    
        Optional<Solicitud> findByIdContenedor(Long idContenedor);

;
}
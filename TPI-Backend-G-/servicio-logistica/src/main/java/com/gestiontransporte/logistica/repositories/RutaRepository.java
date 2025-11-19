package com.gestiontransporte.logistica.repositories;

import com.gestiontransporte.logistica.models.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RutaRepository extends JpaRepository<Ruta, Long> {

    Optional<Ruta> findByIdSolicitud(Long idSolicitud);
}

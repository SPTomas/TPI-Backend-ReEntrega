package com.gestiontransporte.solicitud.repositories;

import com.gestiontransporte.solicitud.models.Localizacion;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalizacionRepository extends JpaRepository<Localizacion, Long> {


    Optional<Localizacion> findByLatitudAndLongitudAndDescripcion(BigDecimal lat, BigDecimal lon, String desc);

}

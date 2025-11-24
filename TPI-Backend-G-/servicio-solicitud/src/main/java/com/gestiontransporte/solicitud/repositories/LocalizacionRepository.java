package com.gestiontransporte.solicitud.repositories;

import com.gestiontransporte.solicitud.models.Localizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalizacionRepository extends JpaRepository<Localizacion, Long> {
}

package com.gestiontransporte.logistica.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gestiontransporte.logistica.models.Localizacion;

@Repository
public interface LocalizacionRepository extends JpaRepository<Localizacion, Long> {
}

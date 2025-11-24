package com.gestiontransporte.transporte.repositories;

import com.gestiontransporte.transporte.models.Precios;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreciosRepository extends JpaRepository<Precios, Long> {
}

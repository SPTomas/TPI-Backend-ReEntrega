package com.gestiontransporte.logistica.repositories;

import com.gestiontransporte.logistica.models.Ruta;
import com.gestiontransporte.logistica.models.Tramo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TramoRepository extends JpaRepository<Tramo, Long> {

    void deleteByRuta(Ruta ruta);

    List<Tramo> findByRutaOrderByIdTramoAsc(Ruta ruta);
}

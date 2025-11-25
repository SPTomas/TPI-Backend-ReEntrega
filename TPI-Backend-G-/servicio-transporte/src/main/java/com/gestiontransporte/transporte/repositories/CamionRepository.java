package com.gestiontransporte.transporte.repositories;

import com.gestiontransporte.transporte.models.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface CamionRepository extends JpaRepository<Camion, String> {

    // - save(Camion camion)
    // - findById(String patente)
    // - findAll()
    // - deleteById(String patente)
}
package com.gestiontransporte.transporte.repositories;

import com.gestiontransporte.transporte.models.Camion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de Spring Data JPA para la entidad Camion.
 *
 * Fíjate en la firma: JpaRepository<Camion, String>
 * El segundo tipo es 'String' porque la clave primaria (@Id)
 * de Camion (la patente) es un String, no un Long.
 */
@Repository
public interface CamionRepository extends JpaRepository<Camion, String> {

    // Spring Data JPA implementará automáticamente:
    // - save(Camion camion)
    // - findById(String patente)
    // - findAll()
    // - deleteById(String patente)
    // - ...y muchos más!
}
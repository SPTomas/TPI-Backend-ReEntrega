package com.gestiontransporte.transporte.repositories;

import com.gestiontransporte.transporte.models.Transportista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TransportistaRepository extends JpaRepository<Transportista, Long> {

}
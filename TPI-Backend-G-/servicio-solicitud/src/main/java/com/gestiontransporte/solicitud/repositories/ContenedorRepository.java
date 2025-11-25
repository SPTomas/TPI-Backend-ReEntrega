package com.gestiontransporte.solicitud.repositories;

import com.gestiontransporte.solicitud.models.Contenedor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ContenedorRepository extends JpaRepository<Contenedor, Long> {

    List<Contenedor> findByEstadoIn(List<String> estados);

    List<Contenedor> findByEstadoInAndIdCliente(List<String> estados, Long idCliente);

}
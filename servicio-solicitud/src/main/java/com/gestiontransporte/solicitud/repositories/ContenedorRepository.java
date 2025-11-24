package com.gestiontransporte.solicitud.repositories;

import com.gestiontransporte.solicitud.models.Contenedor;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de Spring Data JPA para la entidad Contenedor.
 * Hereda todos los métodos CRUD básicos (save, findById, findAll, etc.)
 * para la entidad Contenedor, cuya clave primaria es de tipo Long.
 */
@Repository
public interface ContenedorRepository extends JpaRepository<Contenedor, Long> {


    List<Contenedor> findByEstadoIn(List<String> estados);

    List<Contenedor> findByEstadoInAndIdCliente(List<String> estados, Long idCliente);

    // Spring Data JPA implementará automáticamente los métodos.
}
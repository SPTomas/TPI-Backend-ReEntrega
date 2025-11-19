package com.gestiontransporte.transporte.repositories;

import com.gestiontransporte.transporte.models.Transportista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio de Spring Data JPA para la entidad Transportista.
 * Hereda todos los métodos CRUD básicos (save, findById, findAll, deleteById)
 * y también soporta paginación y ordenamiento.
 */
@Repository
public interface TransportistaRepository extends JpaRepository<Transportista, Long> {

    // Spring Data JPA implementará automáticamente los métodos CRUD.

    // (Opcional) Si quisieras filtrar por 'estado' para el soft-delete,
    // podrías agregar esto:
    //
    // import org.springframework.data.jpa.repository.Query;
    // import java.util.List;
    //
    // @Query("SELECT t FROM Transportista t WHERE t.estado = 'ACTIVO'")
    // List<Transportista> findAllActivos();
}
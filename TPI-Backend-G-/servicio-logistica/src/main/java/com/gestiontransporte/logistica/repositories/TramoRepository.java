package com.gestiontransporte.logistica.repositories;

import com.gestiontransporte.logistica.models.Ruta;
import com.gestiontransporte.logistica.models.Tramo;
import com.gestiontransporte.logistica.models.EstadoTramo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TramoRepository extends JpaRepository<Tramo, Long> {

    void deleteByRuta(Ruta ruta);

    List<Tramo> findByRutaOrderByIdTramoAsc(Ruta ruta);

    List<Tramo> findByPatenteCamion(String patenteCamion);

    List<Tramo> findByPatenteCamionAndEstado(String patenteCamion, EstadoTramo estado);

    List<Tramo> findByEstadoAndPatenteCamionIsNull(EstadoTramo estado);
}

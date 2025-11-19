package com.gestiontransporte.logistica.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Long idRuta;

    @Column(name = "id_solicitud", nullable = false, unique = true) // Una ruta por solicitud
    private Long idSolicitud;

    @Column(name = "latitud_origen")
    private BigDecimal latitudOrigen;
    @Column(name = "longitud_origen")
    private BigDecimal longitudOrigen;
    @Column(name = "latitud_destino")
    private BigDecimal latitudDestino;
    @Column(name = "longitud_destino")
    private BigDecimal longitudDestino;

    @Column(name = "cantidad_tramos")
    private Integer cantidadTramos;
    @Column(name = "cantidad_depositos")
    private Integer cantidadDepositos;

        @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL)
        @JsonManagedReference
        private List<Tramo> tramos;
}
package com.gestiontransporte.logistica.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "ruta")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ruta")
    private Long idRuta;

    @Column(name = "id_solicitud", nullable = false, unique = true)
    private Long idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_origen")
    private Localizacion origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_destino")
    private Localizacion destino;

    @Column(name = "cantidad_tramos")
    private Integer cantidadTramos;

    @Column(name = "cantidad_depositos")
    private Integer cantidadDepositos;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Tramo> tramos;
}

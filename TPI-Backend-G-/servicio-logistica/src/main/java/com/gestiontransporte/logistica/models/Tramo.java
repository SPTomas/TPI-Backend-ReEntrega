package com.gestiontransporte.logistica.models;


import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tramo")
public class Tramo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tramo")
    private Long idTramo;

    @ManyToOne
    @JoinColumn(name = "id_ruta", nullable = false) 
    @JsonBackReference
    private Ruta ruta;

    @ManyToOne
    @JoinColumn(name = "deposito_origen") 
    private Deposito depositoOrigen;
    
    @ManyToOne
    @JoinColumn(name = "deposito_destino") 
    private Deposito depositoDestino;

    private String tipo; 
    

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoTramo estado = EstadoTramo.ESTIMADO;

    @Column(name = "costo_aproximado")
    private BigDecimal costoAproximado;
    @Column(name = "costo_real")
    private BigDecimal costoReal;

    @Column(name = "fecha_hora_inicio")
    private LocalDateTime fechaHoraInicio;
    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @Column(name = "patente_camion") 
    private String patenteCamion;

    @Column(name = "duracion_min")
    private BigDecimal tiempoEstimado;


    @Column(name = "descripcion_origen")
    private String descripcionOrigen;

    @Column(name = "descripcion_destino")
    private String descripcionDestino;    

    @Column(name = "distancia_km", precision = 10, scale = 3)
    private BigDecimal distanciaKm; 

}
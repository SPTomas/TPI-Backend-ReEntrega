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
    @JoinColumn(name = "id_ruta", nullable = false) // FK a Ruta
    @JsonBackReference
    private Ruta ruta;

    @ManyToOne
    @JoinColumn(name = "deposito_origen") // FK a Deposito (puede ser nulo si es origen)
    private Deposito depositoOrigen;
    
    @ManyToOne
    @JoinColumn(name = "deposito_destino") // FK a Deposito (puede ser nulo si es destino final)
    private Deposito depositoDestino;

    private String tipo; // (origen-deposito, deposito-deposito, deposito-destino)
    
    @Column(name = "estado")
    private String estado = "SIN_EMPEZAR";

      /**
     * Valores esperados (ejemplo): SIN_EMPEZAR" - "EN_TRASLADO" - "FINALIZADO"
     */

    @Column(name = "costo_aproximado")
    private BigDecimal costoAproximado;
    @Column(name = "costo_real")
    private BigDecimal costoReal;

    @Column(name = "fecha_hora_inicio")
    private LocalDateTime fechaHoraInicio;
    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @Column(name = "patente_camion") // Guardamos la FK (patente)
    private String patenteCamion;

    @Column(name = "duracion_min")
    private BigDecimal tiempoEstimado;

}
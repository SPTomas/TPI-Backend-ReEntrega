package com.gestiontransporte.logistica.models;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Entity
@Table(name = "localizacion")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class Localizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_localizacion")
    private Long idLocalizacion;

    private BigDecimal latitud;
    private BigDecimal longitud;

    private String descripcion;
}

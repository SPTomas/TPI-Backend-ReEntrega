package com.gestiontransporte.solicitud.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "localizacion")
public class Localizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_localizacion")
    private Long idLocalizacion;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal longitud;

    @Column(name = "descripcion")
    private String descripcion;
}

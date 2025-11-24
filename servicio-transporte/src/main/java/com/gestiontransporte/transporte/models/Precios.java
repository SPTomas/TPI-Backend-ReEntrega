package com.gestiontransporte.transporte.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "precios")
public class Precios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Precio por litro de gasoil
    @Column(name = "precio_gasoil", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioGasoil;
}

package com.gestiontransporte.transporte.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "camion")
public class Camion {

    @Id
    @Column(name = "patente")
    private String patente;

    @ManyToOne
    @JoinColumn(name = "id_transportista", nullable = false)
    private Transportista transportista;

    @Column(name = "capacidad_peso", precision = 10, scale = 2) 
    private BigDecimal capacidadPeso; 

    @Column(name = "capacidad_volumen", precision = 10, scale = 2) 
    private BigDecimal capacidadVolumen; 

    @Column(name = "disponibilidad")
    private Boolean disponibilidad; 

    @Column(name = "consumo_real_litros_km", precision = 10, scale = 3)
    private BigDecimal consumoRealLitrosKm;

    @Column(name = "estado")
    private String estado;
}
package com.gestiontransporte.transporte.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "camion")
public class Camion {

    /**
     * Clave Primaria (PK) - No es autoincremental.
     * Es la patente (ej. "AB123CD") que se asigna manualmente.
     *
     */
    @Id
    @Column(name = "patente")
    private String patente;

    /**
     * Relación Muchos-a-Uno con Transportista.
     * Un transportista puede tener muchos camiones.
     * Un camión pertenece a un solo transportista.
     *
     */
    @ManyToOne
    @JoinColumn(name = "id_transportista", nullable = false) // Define la columna FK
    private Transportista transportista;

    @Column(name = "capacidad_peso", precision = 10, scale = 2) // ej. 10000.00 kg
    private BigDecimal capacidadPeso; //

    @Column(name = "capacidad_volumen", precision = 10, scale = 2) // ej. 80.00 m3
    private BigDecimal capacidadVolumen; //

    @Column(name = "disponibilidad")
    private Boolean disponibilidad; //

    @Column(name = "consumo_real_litros_km", precision = 10, scale = 3)
    private BigDecimal consumoRealLitrosKm;

    @Column(name = "estado")
    private String estado;
}
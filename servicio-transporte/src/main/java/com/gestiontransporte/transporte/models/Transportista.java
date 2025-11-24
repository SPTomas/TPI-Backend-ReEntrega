package com.gestiontransporte.transporte.models;

import jakarta.persistence.*;
import lombok.Data;

@Data // Anotación de Lombok para getters, setters, etc.
@Entity
@Table(name = "transportista")
public class Transportista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transportista") // Coincide con el DER
    private Long idTransportista;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "telefono")
    private String telefono;

    /**
     * Campo para el Soft-Delete (ej. "ACTIVO", "ARCHIVADO")
     * (Basado en el Swagger)
     */
    @Column(name = "estado")
    private String estado;
}
package com.gestiontransporte.solicitud.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long idCliente;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    private String telefono;

    @Column(name = "documento")
    private String documento;

    // Para linkear con Keycloak más adelante si querés
    @Column(name = "id_keycloak")
    private String idKeycloak;
}

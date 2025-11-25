package com.gestiontransporte.solicitud.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;


@Data
@Entity
@Table(name = "contenedor")
public class Contenedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contenedor")
    private Long idContenedor;

    @Column(name = "peso")
    private BigDecimal peso;

    @Column(name = "volumen")
    private BigDecimal volumen;

    @Column(name = "estado")
    private String estado;

    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;
}
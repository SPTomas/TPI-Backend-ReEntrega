package com.gestiontransporte.solicitud.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "solicitud")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long numero;

    @Column(name = "tiempo_estimado")
    private Long tiempoEstimado;

    @Column(name = "tiempo_real")
    private Long tiempoReal;

    @Column(name = "costo_estimado")
    private BigDecimal costoEstimado;

    @Column(name = "costo_final")
    private BigDecimal costoFinal;

    @Column(name = "id_contenedor")
    private Long idContenedor;

    @Column(name = "id_cliente")
    private Long idCliente;

    @Column(name = "estado")
    private String estado;

    // 🔹 NUEVO: FKs a Localizacion

    @ManyToOne
    @JoinColumn(name = "id_origen")
    private Localizacion origen;

    @ManyToOne
    @JoinColumn(name = "id_destino")
    private Localizacion destino;
}

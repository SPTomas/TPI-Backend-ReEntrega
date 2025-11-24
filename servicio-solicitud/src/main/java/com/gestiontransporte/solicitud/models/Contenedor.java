package com.gestiontransporte.solicitud.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Modelo de Entidad (JPA) para Contenedor.
 * Basado en el DER del proyecto y el enunciado.
 */
@Data
@Entity
@Table(name = "contenedor")
public class Contenedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contenedor")
    private Long idContenedor;

    /**
     * Peso del contenedor (restricción para camiones).
     */
    @Column(name = "peso")
    private BigDecimal peso;

    /**
     * Volumen del contenedor (restricción para camiones).
     */
    @Column(name = "volumen")
    private BigDecimal volumen;

    /**
     * Estado del contenedor (para seguimiento).
     * Ej: "PENDIENTE_RETIRO", "EN_VIAJE", "EN_DEPOSITO", "ENTREGADO".
     */
    @Column(name = "estado")
    private String estado;

    /**
     * FK del cliente asociado.
     * Guardamos solo el ID, ya que la entidad 'Cliente'
     * será gestionada por el microservicio de Usuarios.
     */
    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;
}
package com.gestiontransporte.solicitud.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Representa la entidad Solicitud, mapeada a la tabla 'solicitud' 
 * en la base de datos, según el DER del proyecto.
 * [cite: 555]
 */
@Data // Anotación de Lombok para generar getters, setters, etc. [cite: 281]
@Entity // Marca esta clase como una entidad persistente 
@Table(name = "solicitud")
public class Solicitud {

    @Id // Marca este campo como la clave primaria [cite: 290, 565]
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Asume que el ID es autoincremental 
    private Long numero; // 

    @Column(name = "tiempo_estimado") // Mapea al nombre de columna snake_case [cite: 279]
    private Long tiempoEstimado; //  (Usamos Long para guardar minutos/segundos)

    @Column(name = "tiempo_real")
    private Long tiempoReal; // [cite: 567]

    @Column(name = "costo_estimado")
    private BigDecimal costoEstimado; //  (BigDecimal es ideal para moneda)

    @Column(name = "costo_final")
    private BigDecimal costoFinal; // [cite: 569]

    // --- Claves Foráneas (Foreign Keys) ---

    /**
     * FK que referencia al Contenedor.
     * En este microservicio, solo guardamos el ID.
     * El microservicio de Logística/Transporte gestionará la entidad Contenedor.
     */
    @Column(name = "id_contenedor")
    private Long idContenedor; // 

    /**
     * FK que referencia al Cliente.
     * En este microservicio, solo guardamos el ID.
     * El ServicioUsuarios gestionará la entidad Cliente. 
     */
    @Column(name = "id_cliente")
    private Long idCliente; // 

    @Column(name = "estado") // Hibernate creará esta columna
    private String estado;
}
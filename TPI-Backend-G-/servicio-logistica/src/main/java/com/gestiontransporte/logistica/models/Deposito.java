package com.gestiontransporte.logistica.models;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "deposito")
public class Deposito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deposito")
    private Long idDeposito;

    @Column(nullable = false)
    private String nombre;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String calle;
    private String numero;


        @Column(name = "tarifa_estadia_dia")
    private BigDecimal tarifaEstadiaDia;
  
}
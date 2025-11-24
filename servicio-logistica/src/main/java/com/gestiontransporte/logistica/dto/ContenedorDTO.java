package com.gestiontransporte.logistica.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ContenedorDTO {
    private Long idContenedor;
    private Long idCliente;
    private BigDecimal peso;
    private BigDecimal volumen;
}

package com.gestiontransporte.solicitud.models; 
public class EstadoContenedorDTO {

    private Long idContenedor;
    private String estado;

    public EstadoContenedorDTO(Long idContenedor, String estado) {
        this.idContenedor = idContenedor;
        this.estado = estado;
    }

    public Long getIdContenedor() {
        return idContenedor;
    }

    public String getEstado() {
        return estado;
    }
}

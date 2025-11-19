package com.gestiontransporte.logistica.dto;

import java.util.List;

public class CrearRutaRequestDTO {

    private Long idSolicitud;

    private PuntoRutaDTO origen;
    private List<PuntoRutaDTO> puntosIntermedios; // puede ser null o lista vacía
    private PuntoRutaDTO destino;

    public Long getIdSolicitud() { return idSolicitud; }
    public void setIdSolicitud(Long idSolicitud) { this.idSolicitud = idSolicitud; }

    public PuntoRutaDTO getOrigen() { return origen; }
    public void setOrigen(PuntoRutaDTO origen) { this.origen = origen; }

    public List<PuntoRutaDTO> getPuntosIntermedios() { return puntosIntermedios; }
    public void setPuntosIntermedios(List<PuntoRutaDTO> puntosIntermedios) {
        this.puntosIntermedios = puntosIntermedios;
    }

    public PuntoRutaDTO getDestino() { return destino; }
    public void setDestino(PuntoRutaDTO destino) { this.destino = destino; }

    @Override
    public String toString() {
        return "CrearRutaRequestDTO{" +
                "idSolicitud=" + idSolicitud +
                ", origen=" + origen +
                ", puntosIntermedios=" + puntosIntermedios +
                ", destino=" + destino +
                '}';
    }
}

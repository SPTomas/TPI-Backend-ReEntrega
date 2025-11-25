package com.gestiontransporte.logistica.dto;

import java.util.List;

public class RutasAlternativasRequestDTO {

    private Long idSolicitud;
    private PuntoRutaDTO origen;
    private PuntoRutaDTO destino;

    private List<List<Long>> alternativasDepositos;

    public Long getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Long idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public PuntoRutaDTO getOrigen() {
        return origen;
    }

    public void setOrigen(PuntoRutaDTO origen) {
        this.origen = origen;
    }

    public PuntoRutaDTO getDestino() {
        return destino;
    }

    public void setDestino(PuntoRutaDTO destino) {
        this.destino = destino;
    }

    public List<List<Long>> getAlternativasDepositos() {
        return alternativasDepositos;
    }

    public void setAlternativasDepositos(List<List<Long>> alternativasDepositos) {
        this.alternativasDepositos = alternativasDepositos;
    }
}

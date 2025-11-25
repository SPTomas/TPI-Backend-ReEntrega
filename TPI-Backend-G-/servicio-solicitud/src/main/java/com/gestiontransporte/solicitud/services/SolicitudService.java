package com.gestiontransporte.solicitud.services;

import com.gestiontransporte.solicitud.repositories.SolicitudRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.http.*;

import com.gestiontransporte.solicitud.services.ContenedorService; 
import com.gestiontransporte.solicitud.services.ClienteService;
import com.gestiontransporte.solicitud.models.*;
import com.gestiontransporte.solicitud.repositories.SolicitudRepository;
import com.gestiontransporte.solicitud.repositories.LocalizacionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;


import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final ContenedorService contenedorService;
    private final RestTemplate restTemplate;
    private final ClienteService clienteService;
    private final LocalizacionRepository localizacionRepository; 

    @Value("${api.servicios.logistica}")
    private String logisticaBaseUrl;

    public SolicitudService(SolicitudRepository solicitudRepository,
                            ContenedorService contenedorService,
                            RestTemplate restTemplate,
                            ClienteService clienteService,
                            LocalizacionRepository localizacionRepository) {
        this.solicitudRepository = solicitudRepository;
        this.contenedorService = contenedorService;
        this.restTemplate = restTemplate;
        this.clienteService = clienteService;
        this.localizacionRepository = localizacionRepository; 
    }

    public Optional<Solicitud> buscarSolicitudPorId(Long numero) {
        return solicitudRepository.findById(numero);
    }

    public List<Solicitud> buscarTodasLasSolicitudes() {
        return solicitudRepository.findAll();
    }

   public Solicitud crearSolicitud(SolicitudRequestDTO solicitudDTO) {
    if (solicitudDTO.getPesoContenedor() == null || solicitudDTO.getVolumenContenedor() == null) {
        throw new IllegalArgumentException("pesoContenedor y volumenContenedor son obligatorios.");
    }
    if (solicitudDTO.getOrigen() == null || solicitudDTO.getDestino() == null) {
        throw new IllegalArgumentException("Debe enviar origen y destino.");
    }
    Long idCliente;
    if (solicitudDTO.getIdCliente() != null) {
        idCliente = clienteService.buscarPorId(solicitudDTO.getIdCliente())
                .map(Cliente::getIdCliente)
                .orElseThrow(() ->
                        new IllegalArgumentException("No existe cliente con id " + solicitudDTO.getIdCliente()));
    } else {
        var cliente = clienteService.obtenerOCrearPorEmail(
                solicitudDTO.getEmailCliente(),
                solicitudDTO.getNombreCliente(),
                solicitudDTO.getTelefonoCliente()
        );
        idCliente = cliente.getIdCliente();
    }
    Contenedor contenedorNuevo = new Contenedor();
    contenedorNuevo.setIdCliente(idCliente);
    contenedorNuevo.setPeso(solicitudDTO.getPesoContenedor());
    contenedorNuevo.setVolumen(solicitudDTO.getVolumenContenedor());

    Contenedor contenedorGuardado = contenedorService.crearContenedor(contenedorNuevo);

    Localizacion origen = obtenerOCrearLocalizacion(solicitudDTO.getOrigen());
    Localizacion destino = obtenerOCrearLocalizacion(solicitudDTO.getDestino());

    Solicitud solicitudNueva = new Solicitud();
    solicitudNueva.setIdCliente(idCliente);
    solicitudNueva.setIdContenedor(contenedorGuardado.getIdContenedor());
    solicitudNueva.setEstado("CREADA");
    solicitudNueva.setCostoEstimado(solicitudDTO.getCostoEstimado());
    solicitudNueva.setTiempoEstimado(solicitudDTO.getTiempoEstimado());

    solicitudNueva.setOrigen(origen);
    solicitudNueva.setDestino(destino);

    return solicitudRepository.save(solicitudNueva);
}

    public boolean borrarSolicitud(Long numero) {
        if (solicitudRepository.existsById(numero)) {
            solicitudRepository.deleteById(numero);
            return true;
        }
        return false;
    }
    
    public Optional<Solicitud> reemplazarSolicitud(Long numero, Solicitud solicitudNueva) {
        return solicitudRepository.findById(numero)
            .map(solicitudActual -> {
                solicitudActual.setIdCliente(solicitudNueva.getIdCliente());
                solicitudActual.setIdContenedor(solicitudNueva.getIdContenedor());
                solicitudActual.setTiempoEstimado(solicitudNueva.getTiempoEstimado());
                solicitudActual.setTiempoReal(solicitudNueva.getTiempoReal());
                solicitudActual.setCostoEstimado(solicitudNueva.getCostoEstimado());
                solicitudActual.setCostoFinal(solicitudNueva.getCostoFinal());
                
                return solicitudRepository.save(solicitudActual);
            });
    }

public Optional<Solicitud> actualizarParcial(Long numero, java.util.Map<String, Object> updates) {
    return solicitudRepository.findById(numero)
        .map(solicitudActual -> {

            if (updates.containsKey("costoEstimado")) {
                solicitudActual.setCostoEstimado(
                    new java.math.BigDecimal(updates.get("costoEstimado").toString())
                );
            }

            if (updates.containsKey("tiempoEstimado")) {
                solicitudActual.setTiempoEstimado(
                    Long.parseLong(updates.get("tiempoEstimado").toString())
                );
            }

            if (updates.containsKey("costoFinal")) {
                solicitudActual.setCostoFinal(
                    new java.math.BigDecimal(updates.get("costoFinal").toString())
                );
            }

            if (updates.containsKey("tiempoReal")) {
                solicitudActual.setTiempoReal(
                    Long.parseLong(updates.get("tiempoReal").toString())
                );
            }

            if (updates.containsKey("estado")) {
                solicitudActual.setEstado(updates.get("estado").toString());
            }

            Solicitud guardada = solicitudRepository.save(solicitudActual);

            if (updates.containsKey("estado")) {
                sincronizarEstadoContenedorConSolicitud(guardada);
            }

            return guardada;
        });
}

    public Optional<Solicitud> cancelarSolicitud(Long numero) {
        return solicitudRepository.findById(numero)
            .map(solicitudActual -> {
                

                solicitudActual.setEstado("CANCELADA");
                return solicitudRepository.save(solicitudActual);
                
            });
    }

public Optional<Solicitud> finalizarSolicitud(Long numero, FinalizarSolicitudDTO dto) {
    return solicitudRepository.findById(numero)
        .map(solicitudActual -> {

            if (dto.getCostoFinal() != null) {
                solicitudActual.setCostoFinal(dto.getCostoFinal());
            }
            if (dto.getTiempoReal() != null) {
                solicitudActual.setTiempoReal(dto.getTiempoReal());
            }
            if (dto.getEstado() != null) {
                solicitudActual.setEstado(dto.getEstado());
            } else {
                solicitudActual.setEstado("COMPLETADA");
            }

            Solicitud guardada = solicitudRepository.save(solicitudActual);

            sincronizarEstadoContenedorConSolicitud(guardada);

            return guardada;
        });
}

public SeguimientoSolicitudDTO obtenerSeguimiento(Long numero) {

    Solicitud solicitud = solicitudRepository.findById(numero).orElse(null);
    if (solicitud == null) {
        return null;
    }

    SeguimientoSolicitudDTO dto = new SeguimientoSolicitudDTO();

    dto.setNumeroSolicitud(solicitud.getNumero());
    dto.setEstadoSolicitud(solicitud.getEstado());
    dto.setCostoEstimado(solicitud.getCostoEstimado());
    dto.setCostoFinal(solicitud.getCostoFinal());
    dto.setTiempoEstimado(solicitud.getTiempoEstimado());
    dto.setTiempoReal(solicitud.getTiempoReal());

    String urlRuta = logisticaBaseUrl + "/rutas/solicitud/" + numero;

    RutaLogisticaDTO ruta;
    try {
        ruta = restTemplate.getForObject(urlRuta, RutaLogisticaDTO.class);
    } catch (Exception e) {
        return dto;
    }

    if (ruta == null) {
        return dto;
    }

    dto.setCantidadTramos(ruta.getCantidadTramos());
    dto.setCantidadDepositos(ruta.getCantidadDepositos());

    if (ruta.getTramos() == null || ruta.getTramos().isEmpty()) {
        dto.setEstadoRuta("NO_INICIADA");
        dto.setTramos(new ArrayList<>());
        dto.setTramoActual(null);
        return dto;
    }
    List<TramoSeguimientoDTO> tramosSeguimiento = ruta.getTramos()
            .stream()
            .map(t -> {
                TramoSeguimientoDTO tDto = new TramoSeguimientoDTO();
                tDto.setIdTramo(t.getIdTramo());
                tDto.setTipo(t.getTipo());
                tDto.setEstado(t.getEstado());
                tDto.setFechaHoraInicio(t.getFechaHoraInicio());
                tDto.setFechaHoraFin(t.getFechaHoraFin());
                tDto.setPatenteCamion(t.getPatenteCamion());

                if (t.getDepositoOrigen() != null) {
                    tDto.setDepositoOrigen(t.getDepositoOrigen().getNombre());
                }
                if (t.getDepositoDestino() != null) {
                    tDto.setDepositoDestino(t.getDepositoDestino().getNombre());
                }

                return tDto;
            })
            .sorted(Comparator.comparing(TramoSeguimientoDTO::getIdTramo))  
            .collect(Collectors.toList());

    dto.setTramos(tramosSeguimiento);

    boolean todosFinalizados = tramosSeguimiento.stream()
            .allMatch(t -> "FINALIZADO".equalsIgnoreCase(t.getEstado()));

    boolean algunoEnTraslado = tramosSeguimiento.stream()
            .anyMatch(t -> "EN_TRASLADO".equalsIgnoreCase(t.getEstado()));

    if (todosFinalizados) {
        dto.setEstadoRuta("FINALIZADA");
    } else if (algunoEnTraslado) {
        dto.setEstadoRuta("EN_CURSO");
    } else {
        dto.setEstadoRuta("NO_INICIADA");
    }

    TramoSeguimientoDTO tramoActual = tramosSeguimiento.stream()
            .filter(t -> !"FINALIZADO".equalsIgnoreCase(t.getEstado()))
            .findFirst()
            .orElse(null);

    dto.setTramoActual(tramoActual);

    return dto;
}

private void sincronizarEstadoContenedorConSolicitud(Solicitud solicitud) {
    if (solicitud.getIdContenedor() == null) {
        return;
    }

    String estadoSolicitud = solicitud.getEstado();
    String estadoContenedor;

    switch (estadoSolicitud) {
        case "CREADA":
            estadoContenedor = "PENDIENTE_ENTREGA";
            break;
        case "EN_TRANSITO":
            estadoContenedor = "EN_VIAJE";
            break;
        case "COMPLETADA":
            estadoContenedor = "ENTREGADO_EN_DESTINO";
            break;
        case "CANCELADO":
            estadoContenedor = "CANCELADO";
            break;
        default:
            return;
    }

    contenedorService.actualizarEstado(
            solicitud.getIdContenedor(),
            estadoContenedor
    );
}



private Localizacion obtenerOCrearLocalizacion(LocalizacionDTO dto) {
    return localizacionRepository
            .findByLatitudAndLongitudAndDescripcion(
                    dto.getLatitud(),
                    dto.getLongitud(),
                    dto.getDescripcion()
            )
            .orElseGet(() -> {
                Localizacion nueva = new Localizacion();
                nueva.setLatitud(dto.getLatitud());
                nueva.setLongitud(dto.getLongitud());
                nueva.setDescripcion(dto.getDescripcion());
                return localizacionRepository.save(nueva);
            });
}


}
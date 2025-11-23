package com.gestiontransporte.solicitud.services;

import com.gestiontransporte.solicitud.models.Solicitud;
import com.gestiontransporte.solicitud.models.SolicitudRequestDTO;
import com.gestiontransporte.solicitud.models.TramoSeguimientoDTO;
import com.gestiontransporte.solicitud.repositories.SolicitudRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.http.*;
import com.gestiontransporte.solicitud.models.Contenedor; // <-- Importa Contenedor
import com.gestiontransporte.solicitud.services.ContenedorService; // <-- Importa el servicio
import com.gestiontransporte.solicitud.models.FinalizarSolicitudDTO;
import com.gestiontransporte.solicitud.models.RutaLogisticaDTO;
import com.gestiontransporte.solicitud.models.SeguimientoSolicitudDTO;
import com.gestiontransporte.solicitud.models.RutaLogisticaDTO;
import com.gestiontransporte.solicitud.models.TramoLogisticaDTO;
import com.gestiontransporte.solicitud.models.DepositoLogisticaDTO;
import com.gestiontransporte.solicitud.models.TramoSeguimientoDTO;
import com.gestiontransporte.solicitud.models.SeguimientoSolicitudDTO;
import com.gestiontransporte.solicitud.models.Cliente; // <-- Importa Contenedor
import com.gestiontransporte.solicitud.services.ClienteService; // <-- Importa el servicio

import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class SolicitudService {
                       

    private final SolicitudRepository solicitudRepository;
    private final ContenedorService contenedorService; // <-- 1. Inyéctalo
    private final RestTemplate restTemplate;   // 👈 NUEVO
    private final ClienteService clienteService;   // 👈 NUEVO

    @Value("${api.servicios.logistica}")
    private String logisticaBaseUrl;  
    


    public SolicitudService(SolicitudRepository solicitudRepository,
                        ContenedorService contenedorService,
                        RestTemplate restTemplate,
                        ClienteService clienteService) {
    this.solicitudRepository = solicitudRepository;
    this.contenedorService = contenedorService;
    this.restTemplate = restTemplate;   // 👈 guardamos el cliente HTTP
    this.clienteService = clienteService;

}
    /**
     * Busca una solicitud por su número (ID).
     * @param numero El ID de la solicitud.
     * @return Un Optional que contiene la solicitud si se encuentra, o vacío si no.
     */
    public Optional<Solicitud> buscarSolicitudPorId(Long numero) {
        // Delega la llamada directamente al repositorio
        return solicitudRepository.findById(numero);
    }

    /**
     * Obtiene todas las solicitudes registradas.
     * @return Una lista de todas las solicitudes.
     */
    public List<Solicitud> buscarTodasLasSolicitudes() {
        // Delega la llamada directamente al repositorio
        return solicitudRepository.findAll();
    }


public Solicitud crearSolicitud(SolicitudRequestDTO solicitudDTO) {

    // 1) Validar datos mínimos
    if (solicitudDTO.getPesoContenedor() == null || solicitudDTO.getVolumenContenedor() == null) {
        throw new IllegalArgumentException("pesoContenedor y volumenContenedor son obligatorios.");
    }

    // O bien viene idCliente, o bien nombre+email
    if (solicitudDTO.getIdCliente() == null &&
            (solicitudDTO.getEmailCliente() == null || solicitudDTO.getNombreCliente() == null)) {
        throw new IllegalArgumentException("Debe enviar idCliente o bien nombreCliente + emailCliente.");
    }

    // 2) Resolver / registrar Cliente
    Long idCliente;
    if (solicitudDTO.getIdCliente() != null) {
        // Buscar cliente ya registrado
        idCliente = clienteService.buscarPorId(solicitudDTO.getIdCliente())
                .map(c -> c.getIdCliente())
                .orElseThrow(() ->
                        new IllegalArgumentException("No existe cliente con id " + solicitudDTO.getIdCliente()));
    } else {
        // Registrar (o reutilizar) cliente por email
        var cliente = clienteService.obtenerOCrearPorEmail(
                solicitudDTO.getEmailCliente(),
                solicitudDTO.getNombreCliente(),
                solicitudDTO.getTelefonoCliente()
        );
        idCliente = cliente.getIdCliente();
    }

    // 3) Crear el Contenedor asociado
    Contenedor contenedorNuevo = new Contenedor();
    contenedorNuevo.setIdCliente(idCliente);
    contenedorNuevo.setPeso(solicitudDTO.getPesoContenedor());
    contenedorNuevo.setVolumen(solicitudDTO.getVolumenContenedor());

    Contenedor contenedorGuardado = contenedorService.crearContenedor(contenedorNuevo);

    // 4) Crear la Solicitud
    Solicitud solicitudNueva = new Solicitud();
    solicitudNueva.setIdCliente(idCliente);
    solicitudNueva.setIdContenedor(contenedorGuardado.getIdContenedor());
    solicitudNueva.setEstado("CREADA");

    solicitudNueva.setCostoEstimado(solicitudDTO.getCostoEstimado());

    return solicitudRepository.save(solicitudNueva);
}

    /**
     * MODIFICADO: Vuelve a ser un "Hard Delete".
     * Borra físicamente la solicitud de la base de datos.
     * @param numero El ID de la solicitud a borrar.
     * @return true si se borró, false si no se encontró.
     */
    public boolean borrarSolicitud(Long numero) {
        // 1. Verifica si la entidad existe
        if (solicitudRepository.existsById(numero)) {
            // 2. Si existe, la borra de la BDD
            solicitudRepository.deleteById(numero);
            return true;
        }
        // 3. Si no existe, devuelve false
        return false;
    }
    /**
     * REEMPLAZA una solicitud existente.
     * @param numero El ID de la solicitud a reemplazar.
     * @param solicitudNueva El objeto con los datos nuevos.
     * @return La solicitud actualizada, o vacío si no se encontró.
     */
    public Optional<Solicitud> reemplazarSolicitud(Long numero, Solicitud solicitudNueva) {
        // Busca la solicitud existente
        return solicitudRepository.findById(numero)
            .map(solicitudActual -> {
                // Actualiza TODOS los campos (lógica de PUT)
                solicitudActual.setIdCliente(solicitudNueva.getIdCliente());
                solicitudActual.setIdContenedor(solicitudNueva.getIdContenedor());
                solicitudActual.setTiempoEstimado(solicitudNueva.getTiempoEstimado());
                solicitudActual.setTiempoReal(solicitudNueva.getTiempoReal());
                solicitudActual.setCostoEstimado(solicitudNueva.getCostoEstimado());
                solicitudActual.setCostoFinal(solicitudNueva.getCostoFinal());
                
                return solicitudRepository.save(solicitudActual);
            });
    }

    /**
     * ACTUALIZA PARCIALMENTE una solicitud (lógica de PATCH).
     * Actualiza solo los campos que vienen en el Map.
     * @param numero El ID de la solicitud a actualizar.
     * @param updates Un Map con los campos a cambiar (ej: {"costoFinal": 1500.0})
     * @return La solicitud actualizada, o vacío si no se encontró.
     */
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

            // 👉 Guardamos primero
            Solicitud guardada = solicitudRepository.save(solicitudActual);

            // 👉 Si el request tocó el estado, sincronizamos el contenedor
            if (updates.containsKey("estado")) {
                sincronizarEstadoContenedorConSolicitud(guardada);
            }

            return guardada;
        });
}





    
    /**
     * Marca una solicitud como "Cancelada".
     * (Actualmente solo la busca, ¡necesita un campo 'estado'!)
     * @param numero El ID de la solicitud a cancelar.
     * @return La solicitud "cancelada", o vacío si no se encontró.
     */
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

            //  sincroniza el contenedor 
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

    // --- Completar datos de la solicitud ---
    dto.setNumeroSolicitud(solicitud.getNumero());
    dto.setEstadoSolicitud(solicitud.getEstado());
    dto.setCostoEstimado(solicitud.getCostoEstimado());
    dto.setCostoFinal(solicitud.getCostoFinal());
    dto.setTiempoEstimado(solicitud.getTiempoEstimado());
    dto.setTiempoReal(solicitud.getTiempoReal());

    // --- Pedir la ruta al micro de logística ---
    // Ej: http://servicio-logistica:8084/api/logistica/rutas/solicitud/3
    String urlRuta = logisticaBaseUrl + "/rutas/solicitud/" + numero;

    RutaLogisticaDTO ruta;
    try {
        ruta = restTemplate.getForObject(urlRuta, RutaLogisticaDTO.class);
    } catch (Exception e) {
        // Si no hay ruta o falló logística, devolvemos solo lo de la solicitud
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

    // Mapear tramos de logística -> tramos de seguimiento
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
            .collect(Collectors.toList());

    dto.setTramos(tramosSeguimiento);

    // Estado de la ruta en base a los tramos
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

    // Tramo actual = primer tramo que NO está FINALIZADO
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
            // si es un estado raro, no tocamos el contenedor
            return;
    }

    contenedorService.actualizarEstado(
            solicitud.getIdContenedor(),
            estadoContenedor
    );
}

}
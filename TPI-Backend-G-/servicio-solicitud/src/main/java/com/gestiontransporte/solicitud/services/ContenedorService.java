package com.gestiontransporte.solicitud.services;

import com.gestiontransporte.solicitud.models.Contenedor;
import com.gestiontransporte.solicitud.repositories.ContenedorRepository;
import com.gestiontransporte.solicitud.repositories.SolicitudRepository;


import com.gestiontransporte.solicitud.models.ContenedorPendienteDTO;
import com.gestiontransporte.solicitud.models.Solicitud;
import com.gestiontransporte.solicitud.models.RutaLogisticaDTO;
import com.gestiontransporte.solicitud.models.TramoLogisticaDTO;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ContenedorService {

    private final ContenedorRepository contenedorRepository;

      private final SolicitudRepository solicitudRepository;
    private final RestTemplate restTemplate;

       @Value("${api.servicios.logistica}")
    private String logisticaBaseUrl;
    public ContenedorService(ContenedorRepository contenedorRepository,
                             SolicitudRepository solicitudRepository,
                             RestTemplate restTemplate) {

        this.contenedorRepository = contenedorRepository;
        this.solicitudRepository = solicitudRepository;
        this.restTemplate = restTemplate;
    }

    public Contenedor crearContenedor(Contenedor contenedor) {
        if (contenedor.getIdCliente() == null || contenedor.getPeso() == null || contenedor.getVolumen() == null) {
            throw new IllegalArgumentException("El ID de Cliente, Peso y Volumen son obligatorios para crear un contenedor.");
        }
        contenedor.setEstado("PENDIENTE_ENTREGA");
        
        return contenedorRepository.save(contenedor);
    }

    public Optional<Contenedor> buscarPorId(Long idContenedor) {
        return contenedorRepository.findById(idContenedor);
    }

    public List<Contenedor> buscarTodos() {
        return contenedorRepository.findAll();
    }

    public Optional<Contenedor> actualizarEstado(Long idContenedor, String nuevoEstado) {
        return contenedorRepository.findById(idContenedor)
            .map(contenedor -> {
                contenedor.setEstado(nuevoEstado);
                return contenedorRepository.save(contenedor);
            });
    }

    public Optional<String> obtenerEstado(Long idContenedor) {
        return contenedorRepository.findById(idContenedor)
                .map(Contenedor::getEstado);
    }

    public List<ContenedorPendienteDTO> buscarPendientes(Long idClienteFiltro, String estadoFiltro) {

        List<String> estadosPendientes = List.of("PENDIENTE_ENTREGA", "EN_VIAJE", "EN_DEPOSITO");

        List<Contenedor> contenedores;

        if (estadoFiltro != null && !estadoFiltro.isBlank()) {
            List<String> estados = List.of(estadoFiltro);
            if (idClienteFiltro != null) {
                contenedores = contenedorRepository.findByEstadoInAndIdCliente(estados, idClienteFiltro);
            } else {
                contenedores = contenedorRepository.findByEstadoIn(estados);
            }
        } else {
            if (idClienteFiltro != null) {
                contenedores = contenedorRepository.findByEstadoInAndIdCliente(estadosPendientes, idClienteFiltro);
            } else {
                contenedores = contenedorRepository.findByEstadoIn(estadosPendientes);
            }
        }

        List<ContenedorPendienteDTO> resultado = new ArrayList<>();

        for (Contenedor c : contenedores) {
            ContenedorPendienteDTO dto = new ContenedorPendienteDTO();
            dto.setIdContenedor(c.getIdContenedor());
            dto.setIdCliente(c.getIdCliente());
            dto.setEstadoContenedor(c.getEstado());

            solicitudRepository.findByIdContenedor(c.getIdContenedor())
                    .ifPresent(sol -> {
                        dto.setNumeroSolicitud(sol.getNumero());
                        dto.setEstadoSolicitud(sol.getEstado());

                        try {
                            String urlRuta = logisticaBaseUrl + "/rutas/solicitud/" + sol.getNumero();
                            RutaLogisticaDTO ruta = restTemplate.getForObject(urlRuta, RutaLogisticaDTO.class);
                            String ubicacion = calcularUbicacionDesdeRuta(ruta);
                            dto.setUbicacion(ubicacion);
                        } catch (Exception e) {
                            dto.setUbicacion(null); 
                        }
                    });

            resultado.add(dto);
        }

        return resultado;
    }

    private String calcularUbicacionDesdeRuta(RutaLogisticaDTO ruta) {
        if (ruta == null || ruta.getTramos() == null || ruta.getTramos().isEmpty()) {
            return "SIN_RUTA";
        }

        List<TramoLogisticaDTO> tramos = ruta.getTramos();

        boolean todosFinalizados = tramos.stream()
                .allMatch(t -> "FINALIZADO".equalsIgnoreCase(t.getEstado()));

        if (todosFinalizados) {
            return "ENTREGADO_EN_DESTINO";
        }

        TramoLogisticaDTO tramoActual = tramos.stream()
                .filter(t -> !"FINALIZADO".equalsIgnoreCase(t.getEstado()))
                .findFirst()
                .orElse(tramos.get(tramos.size() - 1));

        String origen = tramoActual.getDepositoOrigen() != null
                ? tramoActual.getDepositoOrigen().getNombre()
                : "ORIGEN";

        String destino = tramoActual.getDepositoDestino() != null
                ? tramoActual.getDepositoDestino().getNombre()
                : "DESTINO";

        if ("EN_TRASLADO".equalsIgnoreCase(tramoActual.getEstado())) {
            return "En viaje de " + origen + " a " + destino;
        } else if ("ASIGNADO".equalsIgnoreCase(tramoActual.getEstado())) {
            return "Esperando salida desde " + origen;
        } else {
            return "En " + origen;
        }
    }

}
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

/**
 * Capa de Servicio que centraliza la lógica de negocio para los Contenedores.
 */
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

    /**
     * Crea un nuevo contenedor.
     * Asigna un estado inicial "PENDIENTE_RETIRO"[cite: 1325, 1376].
     * @param contenedor El contenedor a crear.
     * @return El contenedor guardado.
     */
    public Contenedor crearContenedor(Contenedor contenedor) {
        // Validación de lógica de negocio
        if (contenedor.getIdCliente() == null || contenedor.getPeso() == null || contenedor.getVolumen() == null) {
            throw new IllegalArgumentException("El ID de Cliente, Peso y Volumen son obligatorios para crear un contenedor.");
        }
        
        // Asignamos un estado inicial para el seguimiento
        contenedor.setEstado("PENDIENTE_ENTREGA");
        
        return contenedorRepository.save(contenedor);
    }

    /**
     * Busca un contenedor por su ID.
     * @param idContenedor El ID del contenedor.
     * @return Un Optional con el contenedor si se encuentra.
     */
    public Optional<Contenedor> buscarPorId(Long idContenedor) {
        return contenedorRepository.findById(idContenedor);
    }

    /**
     * Lista todos los contenedores existentes.
     * @return Lista de contenedores.
     */
    public List<Contenedor> buscarTodos() {
        return contenedorRepository.findAll();
    }

    /**
     * Actualiza el estado de un contenedor (para el seguimiento).
     * @param idContenedor El ID del contenedor a actualizar.
     * @param nuevoEstado El nuevo estado (ej. "EN_VIAJE", "EN_DEPOSITO", "ENTREGADO_EN_DESTINO").
     * @return El contenedor actualizado, o vacío si no se encontró.
     */
    public Optional<Contenedor> actualizarEstado(Long idContenedor, String nuevoEstado) {
        // Buscamos el contenedor por su ID
        return contenedorRepository.findById(idContenedor)
            .map(contenedor -> {
                // Si lo encontramos, actualizamos el estado y guardamos
                contenedor.setEstado(nuevoEstado);
                return contenedorRepository.save(contenedor);
            });
    }

    // (Nota: No implementamos 'delete' porque los contenedores
    // son parte de una solicitud histórica y no deberían borrarse)
    public Optional<String> obtenerEstado(Long idContenedor) {
        return contenedorRepository.findById(idContenedor)
                .map(Contenedor::getEstado);
    }



    public List<ContenedorPendienteDTO> buscarPendientes(Long idClienteFiltro, String estadoFiltro) {

        // Estados considerados "pendientes de entrega"
        List<String> estadosPendientes = List.of("PENDIENTE_ENTREGA", "EN_VIAJE", "EN_DEPOSITO");

        List<Contenedor> contenedores;

        if (estadoFiltro != null && !estadoFiltro.isBlank()) {
            // filtro por un solo estado
            List<String> estados = List.of(estadoFiltro);
            if (idClienteFiltro != null) {
                contenedores = contenedorRepository.findByEstadoInAndIdCliente(estados, idClienteFiltro);
            } else {
                contenedores = contenedorRepository.findByEstadoIn(estados);
            }
        } else {
            // usamos el set de estados "pendientes"
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

            // Buscar la solicitud asociada a este contenedor
            solicitudRepository.findByIdContenedor(c.getIdContenedor())
                    .ifPresent(sol -> {
                        dto.setNumeroSolicitud(sol.getNumero());
                        dto.setEstadoSolicitud(sol.getEstado());

                        // Intentar consultar la ruta para determinar ubicación
                        try {
                            String urlRuta = logisticaBaseUrl + "/rutas/solicitud/" + sol.getNumero();
                            RutaLogisticaDTO ruta = restTemplate.getForObject(urlRuta, RutaLogisticaDTO.class);
                            String ubicacion = calcularUbicacionDesdeRuta(ruta);
                            dto.setUbicacion(ubicacion);
                        } catch (Exception e) {
                            dto.setUbicacion(null); // no pudimos obtener ruta
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

        // Tramo actual = primer tramo no finalizado
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
            // ESTIMADO u otros
            return "En " + origen;
        }
    }

}
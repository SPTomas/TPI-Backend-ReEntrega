package com.gestiontransporte.logistica.services;

import com.gestiontransporte.logistica.dto.CamionDTO;
import com.gestiontransporte.logistica.dto.CrearRutaRequestDTO;
import com.gestiontransporte.logistica.dto.FinalizarSolicitudDTO;
import com.gestiontransporte.logistica.dto.OsrmResponseDTO;
import com.gestiontransporte.logistica.dto.OsrmRouteDTO;
import com.gestiontransporte.logistica.dto.PuntoRutaDTO;
import com.gestiontransporte.logistica.dto.SolicitudDTO;
import com.gestiontransporte.logistica.dto.ContenedorDTO;
import com.gestiontransporte.logistica.models.Deposito;
import com.gestiontransporte.logistica.models.Ruta;
import com.gestiontransporte.logistica.models.Tramo;
import com.gestiontransporte.logistica.repositories.DepositoRepository;
import com.gestiontransporte.logistica.repositories.RutaRepository;
import com.gestiontransporte.logistica.repositories.TramoRepository;

import com.gestiontransporte.logistica.dto.RutaAlternativaDTO;
import com.gestiontransporte.logistica.dto.RutasAlternativasRequestDTO;
import java.util.Comparator;


import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LogisticaService {

    private final RutaRepository rutaRepository;
    private final TramoRepository tramoRepository;
    private final DepositoRepository depositoRepository;
    private final RestTemplate restTemplate;

    @Value("${api.servicios.solicitud}")
    private String solicitudBaseUrl;

    @Value("${api.servicios.transporte}")
    private String transporteBaseUrl;

    @Value("${api.distancia.url}")
    private String distanciaBaseUrl;

    @Value("${api.servicios.contenedor}")
    private String contenedorBaseUrl;


    public LogisticaService(RutaRepository rutaRepository,
                            TramoRepository tramoRepository,
                            DepositoRepository depositoRepository,
                            RestTemplate restTemplate) {
        this.rutaRepository = rutaRepository;
        this.tramoRepository = tramoRepository;
        this.depositoRepository = depositoRepository;
        this.restTemplate = restTemplate;
    }



                        public Tramo cambiarEstadoTramo(Long idTramo, String nuevoEstadoRaw, BigDecimal costoReal) {
                        Tramo tramo = tramoRepository.findById(idTramo)
                                .orElseThrow(() -> new EntityNotFoundException("No existe tramo con id " + idTramo));

                        String nuevoEstado = nuevoEstadoRaw.toUpperCase();

                        switch (nuevoEstado) {

                                case "EN_TRASLADO":
                                // Inicio del tramo
                                if (tramo.getFechaHoraInicio() == null) {
                                        tramo.setFechaHoraInicio(LocalDateTime.now());
                                }
                                tramo.setEstado("EN_TRASLADO");
                                break;

                                case "FINALIZADO":
                                // Fin del tramo
                                if (tramo.getFechaHoraInicio() == null) {
                                        throw new IllegalStateException("No se puede finalizar un tramo que nunca inició");
                                }
                                if (tramo.getFechaHoraFin() == null) {
                                        tramo.setFechaHoraFin(LocalDateTime.now());
                                }
                                tramo.setEstado("FINALIZADO");

                                // 👉 Guardar costo real si vino en el request
                                if (costoReal != null) {
                                        tramo.setCostoReal(costoReal);
                                }
                                break;

                                default:
                                throw new IllegalArgumentException("Estado de tramo no soportado: " + nuevoEstadoRaw);
                        }

                        // Guardamos cambios del tramo
                        tramo = tramoRepository.save(tramo);

                        //  Si recién pasó a EN_TRASLADO, marcamos la solicitud como EN_TRANSITO
                        if ("EN_TRASLADO".equals(tramo.getEstado())) {
                                marcarSolicitudEnTransito(tramo.getRuta());
                        }

                        //  Si recién pasó a FINALIZADO, vemos si ya terminó toda la ruta
                        if ("FINALIZADO".equals(tramo.getEstado())) {
                                procesarSiRutaFinalizada(tramo.getRuta());
                        }

                        return tramo;
                        }

    
    /**
     * Crea una Ruta + 1 Tramo origen-destino para la Solicitud indicada.
     * Por ahora las coordenadas se pasan como parámetro (desde Postman).
     */
        public Ruta crearRutaParaSolicitud(CrearRutaRequestDTO request) {

        Long idSolicitud = request.getIdSolicitud();

        // 1) Validar que la solicitud exista en el micro de Solicitud
        String urlSolicitud = solicitudBaseUrl + "/" + idSolicitud;
        System.out.println(">>> Llamando a servicio-solicitud: " + urlSolicitud);

        try {
                var respSolicitud = restTemplate.getForEntity(urlSolicitud, Void.class);
                if (!respSolicitud.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La solicitud " + idSolicitud + " no existe en servicio-solicitud"
                );
                }
        } catch (HttpClientErrorException e) {
                throw new ResponseStatusException(
                        e.getStatusCode(),
                        "Error al validar la solicitud " + idSolicitud + " en servicio-solicitud",
                        e
                );
        } catch (ResourceAccessException e) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo conectar con servicio-solicitud en " + urlSolicitud,
                        e
                );
        }

        // 2) Validar origen/destino
        if (request.getOrigen() == null || request.getDestino() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Debes enviar origen y destino"
                );
        }

        // 3) Armar la lista de depósitos en orden
        List<Deposito> depositosEnRuta = new ArrayList<>();

        if (request.getPuntosIntermedios() != null) {
                for (PuntoRutaDTO p : request.getPuntosIntermedios()) {
                if (p.getIdDeposito() == null) {
                        // Podés elegir: skip o tirar error. Yo prefiero error explícito:
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Todos los puntos intermedios deben tener idDeposito (los tramos son solo entre depósitos)"
                        );
                }
                Deposito depo = depositoRepository.findById(p.getIdDeposito())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El depósito " + p.getIdDeposito() + " no existe"
                        ));

                depositosEnRuta.add(depo);
                }
        }

        int cantidadDepositos = depositosEnRuta.size();
        // int cantidadTramos = (cantidadDepositos > 1) ? (cantidadDepositos - 1) : 0;
        // NUEVA LÓGICA:
        int cantidadTramos;
        if (cantidadDepositos == 0) {
        // origen -> destino
        cantidadTramos = 1;
        } else {
        // origen -> dep1, entre depósitos, último dep -> destino
        cantidadTramos = cantidadDepositos + 1;
        }
                

        // 4) Ver si ya existe Ruta para esa solicitud
        Ruta ruta = rutaRepository.findByIdSolicitud(idSolicitud).orElse(null);

        if (ruta != null) {
                // Recalcular: borrar tramos viejos
                tramoRepository.deleteByRuta(ruta);
        } else {
                ruta = new Ruta();
                ruta.setIdSolicitud(idSolicitud);
        }

        // Setear datos de Ruta (origen/destino y cantidades)
        ruta.setLatitudOrigen(request.getOrigen().getLatitud());
        ruta.setLongitudOrigen(request.getOrigen().getLongitud());
        ruta.setLatitudDestino(request.getDestino().getLatitud());
        ruta.setLongitudDestino(request.getDestino().getLongitud());
        ruta.setCantidadDepositos(cantidadDepositos);
        ruta.setCantidadTramos(cantidadTramos);

        ruta = rutaRepository.save(ruta);


         BigDecimal costoTotalEstimado = BigDecimal.ZERO;
         BigDecimal tiempoTotalMin = BigDecimal.ZERO;

        PuntoRutaDTO origenDTO = request.getOrigen();
        PuntoRutaDTO destinoDTO = request.getDestino();
    
        if (depositosEnRuta.isEmpty()) {

        OsrmRouteDTO rutaOsrm = consultarDistanciaOsrm(
                origenDTO.getLatitud(),
                origenDTO.getLongitud(),
                destinoDTO.getLatitud(),
                destinoDTO.getLongitud()
        );

        double distanciaMetros = rutaOsrm.getDistance();
        double duracionSegundos = rutaOsrm.getDuration();

        BigDecimal distanciaKm = BigDecimal
                .valueOf(distanciaMetros / 1000.0)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal duracionMin = BigDecimal
                .valueOf(duracionSegundos / 60.0)
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal costoTramo = calcularCostoAproximado(distanciaKm, null);

        costoTotalEstimado = costoTotalEstimado.add(costoTramo);
        tiempoTotalMin = tiempoTotalMin.add(duracionMin);

        Tramo tramo = new Tramo();
        tramo.setRuta(ruta);
        tramo.setDepositoOrigen(null);       // no hay depósito
        tramo.setDepositoDestino(null);      // idem
        tramo.setTipo("origen-destino");
        tramo.setEstado("estimado");
        tramo.setCostoAproximado(costoTramo);
        tramo.setCostoReal(null);
        tramo.setPatenteCamion(null);
        tramo.setTiempoEstimado(duracionMin); // o setTiempoEstimadoMin según el nombre que pusiste

        tramoRepository.save(tramo);

        } else {

        // CASO B: HAY UNO O MÁS DEPÓSITOS

        // B1) Tramo ORIGEN -> PRIMER DEPÓSITO
        Deposito primerDepo = depositosEnRuta.get(0);

        OsrmRouteDTO rutaOsrm1 = consultarDistanciaOsrm(
                origenDTO.getLatitud(),
                origenDTO.getLongitud(),
                primerDepo.getLatitud(),
                primerDepo.getLongitud()
        );

        double distM1 = rutaOsrm1.getDistance();
        double durSeg1 = rutaOsrm1.getDuration();

        BigDecimal distKm1 = BigDecimal
                .valueOf(distM1 / 1000.0)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal durMin1 = BigDecimal
                .valueOf(durSeg1 / 60.0)
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal costoTramo1 = calcularCostoAproximado(distKm1, null);

        costoTotalEstimado = costoTotalEstimado.add(costoTramo1);
        tiempoTotalMin = tiempoTotalMin.add(durMin1);

        Tramo tramo1 = new Tramo();
        tramo1.setRuta(ruta);
        tramo1.setDepositoOrigen(null);          // viene del origen "libre"
        tramo1.setDepositoDestino(primerDepo);   // llega al primer depósito
        tramo1.setTipo("origen-deposito");
        tramo1.setEstado("estimado");
        tramo1.setCostoAproximado(costoTramo1);
        tramo1.setCostoReal(null);
        tramo1.setPatenteCamion(null);
        tramo1.setTiempoEstimado(durMin1);

        tramoRepository.save(tramo1);

        // B2) Tramos DEPOSITO -> DEPOSITO (si hay más de uno)
        for (int i = 0; i < depositosEnRuta.size() - 1; i++) {
                Deposito depOrigen = depositosEnRuta.get(i);
                Deposito depDestino = depositosEnRuta.get(i + 1);

                OsrmRouteDTO rutaOsrm = consultarDistanciaOsrm(
                        depOrigen.getLatitud(),
                        depOrigen.getLongitud(),
                        depDestino.getLatitud(),
                        depDestino.getLongitud()
                );

                double distanciaMetros = rutaOsrm.getDistance();
                double duracionSegundos = rutaOsrm.getDuration();

                BigDecimal distanciaKm = BigDecimal
                        .valueOf(distanciaMetros / 1000.0)
                        .setScale(3, RoundingMode.HALF_UP);

                BigDecimal duracionMin = BigDecimal
                        .valueOf(duracionSegundos / 60.0)
                        .setScale(1, RoundingMode.HALF_UP);

                BigDecimal costoTramo = calcularCostoAproximado(distanciaKm, null);

                costoTotalEstimado = costoTotalEstimado.add(costoTramo);
                tiempoTotalMin = tiempoTotalMin.add(duracionMin);

                Tramo tramo = new Tramo();
                tramo.setRuta(ruta);
                tramo.setDepositoOrigen(depOrigen);
                tramo.setDepositoDestino(depDestino);
                tramo.setTipo("deposito-deposito");
                tramo.setEstado("estimado");
                tramo.setCostoAproximado(costoTramo);
                tramo.setCostoReal(null);
                tramo.setPatenteCamion(null);
                tramo.setTiempoEstimado(duracionMin);

                tramoRepository.save(tramo);
        }

        // B3) Tramo ÚLTIMO DEPÓSITO -> DESTINO
        Deposito ultimoDepo = depositosEnRuta.get(depositosEnRuta.size() - 1);

        OsrmRouteDTO rutaOsrmLast = consultarDistanciaOsrm(
                ultimoDepo.getLatitud(),
                ultimoDepo.getLongitud(),
                destinoDTO.getLatitud(),
                destinoDTO.getLongitud()
        );

        double distMLast = rutaOsrmLast.getDistance();
        double durSegLast = rutaOsrmLast.getDuration();

        BigDecimal distKmLast = BigDecimal
                .valueOf(distMLast / 1000.0)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal durMinLast = BigDecimal
                .valueOf(durSegLast / 60.0)
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal costoTramoLast = calcularCostoAproximado(distKmLast, null);

        costoTotalEstimado = costoTotalEstimado.add(costoTramoLast);
        tiempoTotalMin = tiempoTotalMin.add(durMinLast);

        Tramo tramoLast = new Tramo();
        tramoLast.setRuta(ruta);
        tramoLast.setDepositoOrigen(ultimoDepo);
        tramoLast.setDepositoDestino(null);      // llega al destino "libre"
        tramoLast.setTipo("deposito-destino");
        tramoLast.setEstado("estimado");
        tramoLast.setCostoAproximado(costoTramoLast);
        tramoLast.setCostoReal(null);
        tramoLast.setPatenteCamion(null);
        tramoLast.setTiempoEstimado(durMinLast);

        tramoRepository.save(tramoLast);
        }


        ContenedorDTO contenedorDTO = obtenerContenedorDeSolicitud(idSolicitud);
        BigDecimal tarifaBaseContenedor = calcularPrecioBaseContenedor(contenedorDTO);

        costoTotalEstimado = costoTotalEstimado.add(tarifaBaseContenedor);

        // 6) Actualizar estimación en servicio-solicitud
        actualizarEstimacionSolicitud(idSolicitud, costoTotalEstimado, tiempoTotalMin);

        return ruta;
        }

    /*
     * Usa OSRM para consultar distancia/duración entre dos puntos.
     */
    private OsrmRouteDTO consultarDistanciaOsrm(
            BigDecimal latitudOrigen,
            BigDecimal longitudOrigen,
            BigDecimal latitudDestino,
            BigDecimal longitudDestino
    ) {
        // OJO: OSRM usa LON,LAT
        String url = String.format(
                "%s/route/v1/driving/%s,%s;%s,%s?overview=false",
                distanciaBaseUrl,
                longitudOrigen.toPlainString(),
                latitudOrigen.toPlainString(),
                longitudDestino.toPlainString(),
                latitudDestino.toPlainString()
        );

        ResponseEntity<OsrmResponseDTO> response =
                restTemplate.getForEntity(url, OsrmResponseDTO.class);

        OsrmResponseDTO body = response.getBody();
        if (body == null || body.getRoutes() == null || body.getRoutes().isEmpty()) {
            throw new IllegalStateException("No se pudo obtener ruta desde OSRM");
        }

        return body.getRoutes().get(0);
    }

        private RutaAlternativaDTO calcularRutaAlternativa(
                PuntoRutaDTO origenDTO,
                PuntoRutaDTO destinoDTO,
                List<Deposito> depositosEnRuta,
                String nombreAlternativa
        ) {
        BigDecimal costoTotalEstimado = BigDecimal.ZERO;
        BigDecimal tiempoTotalMin = BigDecimal.ZERO;
        List<String> descripcionTramos = new ArrayList<>();

        // =========================
        // CASO A: SIN DEPÓSITOS (ruta directa)
        // =========================
        if (depositosEnRuta == null || depositosEnRuta.isEmpty()) {

                OsrmRouteDTO rutaOsrm = consultarDistanciaOsrm(
                        origenDTO.getLatitud(),
                        origenDTO.getLongitud(),
                        destinoDTO.getLatitud(),
                        destinoDTO.getLongitud()
                );

                double distanciaMetros = rutaOsrm.getDistance();
                double duracionSegundos = rutaOsrm.getDuration();

                BigDecimal distanciaKm = BigDecimal
                        .valueOf(distanciaMetros / 1000.0)
                        .setScale(3, RoundingMode.HALF_UP);

                BigDecimal duracionMin = BigDecimal
                        .valueOf(duracionSegundos / 60.0)
                        .setScale(1, RoundingMode.HALF_UP);

                BigDecimal costoTramo = calcularCostoAproximado(distanciaKm, null);

                costoTotalEstimado = costoTotalEstimado.add(costoTramo);
                tiempoTotalMin = tiempoTotalMin.add(duracionMin);

                descripcionTramos.add("Origen -> Destino");

                RutaAlternativaDTO dto = new RutaAlternativaDTO();
                dto.setNombre(nombreAlternativa);
                dto.setCostoEstimado(costoTotalEstimado);
                dto.setTiempoEstimadoMin(tiempoTotalMin);
                dto.setCantidadDepositos(0);
                dto.setCantidadTramos(1);
                dto.setDescripcionTramos(descripcionTramos);

                return dto;
        }

        // =========================
        // CASO B: CON DEPÓSITOS
        // =========================

        PuntoRutaDTO origen = origenDTO;
        PuntoRutaDTO destino = destinoDTO;

        // B1) ORIGEN -> PRIMER DEPÓSITO
        Deposito primerDepo = depositosEnRuta.get(0);

        OsrmRouteDTO rutaOsrm1 = consultarDistanciaOsrm(
                origen.getLatitud(),
                origen.getLongitud(),
                primerDepo.getLatitud(),
                primerDepo.getLongitud()
        );

        BigDecimal distKm1 = BigDecimal
                .valueOf(rutaOsrm1.getDistance() / 1000.0)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal durMin1 = BigDecimal
                .valueOf(rutaOsrm1.getDuration() / 60.0)
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal costoTramo1 = calcularCostoAproximado(distKm1, null);

        costoTotalEstimado = costoTotalEstimado.add(costoTramo1);
        tiempoTotalMin = tiempoTotalMin.add(durMin1);

        descripcionTramos.add("Origen -> Depósito " + primerDepo.getNombre());

        // B2) DEPOSITO -> DEPOSITO (si hay más de uno)
        for (int i = 0; i < depositosEnRuta.size() - 1; i++) {
                Deposito depOrigen = depositosEnRuta.get(i);
                Deposito depDestino = depositosEnRuta.get(i + 1);

                OsrmRouteDTO rutaOsrm = consultarDistanciaOsrm(
                        depOrigen.getLatitud(),
                        depOrigen.getLongitud(),
                        depDestino.getLatitud(),
                        depDestino.getLongitud()
                );

                BigDecimal distanciaKm = BigDecimal
                        .valueOf(rutaOsrm.getDistance() / 1000.0)
                        .setScale(3, RoundingMode.HALF_UP);

                BigDecimal duracionMin = BigDecimal
                        .valueOf(rutaOsrm.getDuration() / 60.0)
                        .setScale(1, RoundingMode.HALF_UP);

                BigDecimal costoTramo = calcularCostoAproximado(distanciaKm, null);

                costoTotalEstimado = costoTotalEstimado.add(costoTramo);
                tiempoTotalMin = tiempoTotalMin.add(duracionMin);

                descripcionTramos.add(
                        "Depósito " + depOrigen.getNombre() +
                        " -> Depósito " + depDestino.getNombre()
                );
        }

        // B3) ÚLTIMO DEPÓSITO -> DESTINO
        Deposito ultimoDepo = depositosEnRuta.get(depositosEnRuta.size() - 1);

        OsrmRouteDTO rutaOsrmLast = consultarDistanciaOsrm(
                ultimoDepo.getLatitud(),
                ultimoDepo.getLongitud(),
                destino.getLatitud(),
                destino.getLongitud()
        );

        BigDecimal distKmLast = BigDecimal
                .valueOf(rutaOsrmLast.getDistance() / 1000.0)
                .setScale(3, RoundingMode.HALF_UP);

        BigDecimal durMinLast = BigDecimal
                .valueOf(rutaOsrmLast.getDuration() / 60.0)
                .setScale(1, RoundingMode.HALF_UP);

        BigDecimal costoTramoLast = calcularCostoAproximado(distKmLast, null);

        costoTotalEstimado = costoTotalEstimado.add(costoTramoLast);
        tiempoTotalMin = tiempoTotalMin.add(durMinLast);

        descripcionTramos.add("Depósito " + ultimoDepo.getNombre() + " -> Destino");

        // Armar DTO de salida
        RutaAlternativaDTO dto = new RutaAlternativaDTO();
        dto.setNombre(nombreAlternativa);
        dto.setCostoEstimado(costoTotalEstimado);
        dto.setTiempoEstimadoMin(tiempoTotalMin);
        dto.setCantidadDepositos(depositosEnRuta.size());
        dto.setCantidadTramos(depositosEnRuta.size() + 1);
        dto.setDescripcionTramos(descripcionTramos);

        return dto;
        }


                public List<RutaAlternativaDTO> generarRutasAlternativas(RutasAlternativasRequestDTO request) {

        Long idSolicitud = request.getIdSolicitud();

        // 1) Validar que la solicitud exista (misma lógica que crearRutaParaSolicitud)
        String urlSolicitud = solicitudBaseUrl + "/" + idSolicitud;
        System.out.println(">>> [Alternativas] Llamando a servicio-solicitud: " + urlSolicitud);

        try {
                var respSolicitud = restTemplate.getForEntity(urlSolicitud, Void.class);
                if (!respSolicitud.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La solicitud " + idSolicitud + " no existe en servicio-solicitud"
                );
                }
        } catch (HttpClientErrorException e) {
                throw new ResponseStatusException(
                        e.getStatusCode(),
                        "Error al validar la solicitud " + idSolicitud + " en servicio-solicitud",
                        e
                );
        } catch (ResourceAccessException e) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo conectar con servicio-solicitud en " + urlSolicitud,
                        e
                );
        }

        // 2) Validar origen/destino
        if (request.getOrigen() == null || request.getDestino() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Debes enviar origen y destino"
                );
        }

        PuntoRutaDTO origenDTO = request.getOrigen();
        PuntoRutaDTO destinoDTO = request.getDestino();

        List<RutaAlternativaDTO> alternativas = new ArrayList<>();

        // 3) Siempre generamos la ruta DIRECTA (sin depósitos)
        RutaAlternativaDTO directa = calcularRutaAlternativa(
                origenDTO,
                destinoDTO,
                new ArrayList<>(),
                "Ruta directa"
        );
        alternativas.add(directa);

        // 4) Rutas con depósitos según las combinaciones que mande el cliente
        if (request.getAlternativasDepositos() != null) {
                for (List<Long> idsDepositos : request.getAlternativasDepositos()) {

                if (idsDepositos == null || idsDepositos.isEmpty()) {
                        continue;
                }

                List<Deposito> depositosEnRuta = new ArrayList<>();
                for (Long idDepo : idsDepositos) {
                        Deposito depo = depositoRepository.findById(idDepo)
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "El depósito " + idDepo + " no existe"
                                ));
                        depositosEnRuta.add(depo);
                }

                String nombre = "Depósitos " + idsDepositos;
                RutaAlternativaDTO alternativa = calcularRutaAlternativa(
                        origenDTO,
                        destinoDTO,
                        depositosEnRuta,
                        nombre
                );
                alternativas.add(alternativa);
                }
        }

        // 5) Ordenar por costo estimado (de menor a mayor)
        alternativas.sort(Comparator.comparing(RutaAlternativaDTO::getCostoEstimado));

        return alternativas;
        }

    /**
     * Por ahora un cálculo súper básico: solo base por km.
     * Después diferenciar con tarifas/camión por el enunciado.
     */
    private BigDecimal calcularCostoAproximado(BigDecimal distanciaKm, CamionDTO camionDTO) {
        // Si viene camión, usamos su costo específico, sino un valor base fijo
        BigDecimal costoPorKm;

        if (camionDTO != null && camionDTO.getCostos() != null) {
            costoPorKm = camionDTO.getCostos();
        } else {
            // valor base, ejemplo: 1000 $/km (ajustalo a gusto)
            costoPorKm = BigDecimal.valueOf(1000);
        }

        return distanciaKm.multiply(costoPorKm)
                .setScale(2, RoundingMode.HALF_UP);
    }

    
  public Tramo asignarCamionATramo(Long idTramo, String patenteCamion) {
    Tramo tramo = tramoRepository.findById(idTramo)
            .orElseThrow(() -> new IllegalArgumentException("Tramo " + idTramo + " no existe."));


    // 1) Traer CAMIÓN (Transporte)
    String urlCamion = transporteBaseUrl + "/camiones/" + patenteCamion;

    ResponseEntity<CamionDTO> respCamion =
            restTemplate.getForEntity(urlCamion, CamionDTO.class);

    if (!respCamion.getStatusCode().is2xxSuccessful() || respCamion.getBody() == null) {
        throw new IllegalArgumentException("El camión con patente " + patenteCamion + " no existe.");
    }

    CamionDTO camionDTO = respCamion.getBody();

    if (Boolean.FALSE.equals(camionDTO.getDisponibilidad())) {
        throw new IllegalStateException("El camión " + patenteCamion + " no está disponible.");
    }

    // 2) Traer SOLICITUD asociada a la ruta de este tramo
  
    Ruta ruta = tramo.getRuta();
    if (ruta == null || ruta.getIdSolicitud() == null) {
        throw new IllegalStateException("El tramo no tiene ruta/solicitud asociada.");
    }

    Long idSolicitud = ruta.getIdSolicitud();

    String urlSolicitud = solicitudBaseUrl + "/" + idSolicitud;
    ResponseEntity<SolicitudDTO> respSolicitud =
            restTemplate.getForEntity(urlSolicitud, SolicitudDTO.class);

    if (!respSolicitud.getStatusCode().is2xxSuccessful() || respSolicitud.getBody() == null) {
        throw new IllegalStateException("No se pudo obtener la solicitud " + idSolicitud
                + " desde servicio-solicitud.");
    }

    SolicitudDTO solicitudDTO = respSolicitud.getBody();

    if (solicitudDTO.getIdContenedor() == null) {
        throw new IllegalStateException("La solicitud " + idSolicitud + " no tiene contenedor asociado.");
    }

    Long idContenedor = solicitudDTO.getIdContenedor();

    // ======================
    // 3) Traer CONTENEDOR (peso / volumen)
    // ======================
    String urlContenedor = contenedorBaseUrl + "/" + idContenedor;
    ResponseEntity<ContenedorDTO> respContenedor =
            restTemplate.getForEntity(urlContenedor, ContenedorDTO.class);

    if (!respContenedor.getStatusCode().is2xxSuccessful() || respContenedor.getBody() == null) {
        throw new IllegalStateException("No se pudo obtener el contenedor " + idContenedor
                + " desde servicio-solicitud.");
    }

    ContenedorDTO contenedorDTO = respContenedor.getBody();

    // ======================
    // 4) Validar peso y volumen
    // ======================
    if (contenedorDTO.getPeso() != null && camionDTO.getCapacidadPeso() != null &&
            contenedorDTO.getPeso().compareTo(camionDTO.getCapacidadPeso()) > 0) {

        throw new IllegalStateException(
                "El peso del contenedor (" + contenedorDTO.getPeso() + " kg) supera la capacidad del camión ("
                        + camionDTO.getCapacidadPeso() + " kg).");
    }

    if (contenedorDTO.getVolumen() != null && camionDTO.getCapacidadVolumen() != null &&
            contenedorDTO.getVolumen().compareTo(camionDTO.getCapacidadVolumen()) > 0) {

        throw new IllegalStateException(
                "El volumen del contenedor (" + contenedorDTO.getVolumen() + " m3) supera la capacidad del camión ("
                        + camionDTO.getCapacidadVolumen() + " m3).");
    }

    // ======================
    // 5) Si todo OK → asignar camión al tramo
    // ======================
    tramo.setPatenteCamion(patenteCamion);
    tramo.setEstado("ASIGNADO"); // usá el mismo texto que uses en otros lados

    return tramoRepository.save(tramo);
}

        private void actualizarEstimacionSolicitud(
                Long idSolicitud,
                BigDecimal costoTotalEstimado,
                BigDecimal tiempoTotalMin
        ) 
        {
                // solicitudBaseUrl = "http://servicio-solicitud:8082/solicitudes"
                String url = solicitudBaseUrl + "/" + idSolicitud + "/estimacion";

                Map<String, Object> updates = new HashMap<>();
                updates.put("costoEstimado", costoTotalEstimado);
                updates.put("tiempoEstimado", tiempoTotalMin.longValue()); // minutos

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);

                try {
                        System.out.println(">>> Actualizando estimación de solicitud " + idSolicitud +
                                " costo=" + costoTotalEstimado + " tiempoMin=" + tiempoTotalMin.longValue());
                        restTemplate.postForEntity(url, entity, Void.class);
                        System.out.println(">>> Estimación de solicitud " + idSolicitud + " actualizada OK");
                } catch (Exception e) {
                        System.err.println("No se pudo actualizar estimación de solicitud "
                                + idSolicitud + ": " + e.getMessage());
                }
        }


private void procesarSiRutaFinalizada(Ruta ruta) {
    if (ruta == null) {
        return;
    }
        System.out.println(">>> Verificando si la ruta " + ruta.getIdRuta() + " está finalizada");
    List<Tramo> tramos = tramoRepository.findByRutaOrderByIdTramoAsc(ruta);

    if (tramos.isEmpty()) {
        return;
    }

    boolean todosFinalizados = tramos.stream()
            .allMatch(t -> "FINALIZADO".equalsIgnoreCase(t.getEstado()));

        System.out.println(">>> todosFinalizados = " + todosFinalizados);
    if (!todosFinalizados) {
        return; // todavía falta algún tramo
    }

    // 👉 En este punto: TODOS los tramos de la ruta están FINALIZADOS
    cerrarSolicitudConCostosYTiempo(ruta, tramos);
}


private void cerrarSolicitudConCostosYTiempo(Ruta ruta, List<Tramo> tramos) {
    Long idSolicitud = ruta.getIdSolicitud();
    if (idSolicitud == null) {
                System.err.println(">>> La ruta " + ruta.getIdRuta() + " no tiene idSolicitud asociado");

        return;
    }

    // 1) Costo por tramos (recorrido)
    BigDecimal costoTramos = BigDecimal.ZERO;
    for (Tramo t : tramos) {
        if (t.getCostoReal() != null) {
            costoTramos = costoTramos.add(t.getCostoReal());
        } else if (t.getCostoAproximado() != null) {
            costoTramos = costoTramos.add(t.getCostoAproximado());
        }
    }

    // 2) Costo por estadías en depósitos (por día o fracción)
    BigDecimal costoEstadias = calcularCostoEstadiasPorDia(tramos);

     // 3) Traer contenedor para sumar tarifa base (OPCIONAL según enunciado)
    ContenedorDTO contenedorDTO = null;
    try {
        String urlSolicitud = solicitudBaseUrl + "/" + idSolicitud;
        ResponseEntity<SolicitudDTO> respSol =
                restTemplate.getForEntity(urlSolicitud, SolicitudDTO.class);

        if (respSol.getStatusCode().is2xxSuccessful() && respSol.getBody() != null &&
            respSol.getBody().getIdContenedor() != null) {

            Long idContenedor = respSol.getBody().getIdContenedor();
            String urlCont = contenedorBaseUrl + "/" + idContenedor;

            ResponseEntity<ContenedorDTO> respCont =
                    restTemplate.getForEntity(urlCont, ContenedorDTO.class);

            if (respCont.getStatusCode().is2xxSuccessful()) {
                contenedorDTO = respCont.getBody();
            }
        }
    } catch (Exception e) {
        System.err.println("No se pudo obtener contenedor para la solicitud "
                + idSolicitud + ": " + e.getMessage());
    }

    BigDecimal tarifaBaseContenedor = calcularPrecioBaseContenedor(contenedorDTO);

    // 4) Costo final = tramos + estadías + (base contenedor)
    BigDecimal costoFinal = costoTramos
            .add(costoEstadias)
            .add(tarifaBaseContenedor);

    // 5) Tiempo real total
    Long tiempoRealMin = calcularTiempoRealMinutos(tramos);

    // 6) Mandarlo al micro de solicitud
    actualizarSolicitudComoFinalizada(idSolicitud, costoFinal, tiempoRealMin);
}

private BigDecimal calcularCostoEstadiasPorDia(List<Tramo> tramos) {
    BigDecimal costoTotalEstadias = BigDecimal.ZERO;
    long minutosPorDia = 60L * 24L;

    for (int i = 0; i < tramos.size() - 1; i++) {
        Tramo llegada = tramos.get(i);
        Tramo salida  = tramos.get(i + 1);

        // Debe ser el mismo depósito: se llega y luego se sale
        if (llegada.getDepositoDestino() != null
            && salida.getDepositoOrigen() != null
            && llegada.getDepositoDestino().getIdDeposito()
                    .equals(salida.getDepositoOrigen().getIdDeposito())) {

            if (llegada.getFechaHoraFin() == null || salida.getFechaHoraInicio() == null) {
                continue; // no se puede calcular todavía
            }

            long minutos = java.time.Duration.between(
                    llegada.getFechaHoraFin(),
                    salida.getFechaHoraInicio()
            ).toMinutes();

            if (minutos <= 0) {
                continue;
            }

            // días = ceil(minutos / minutosPorDia)
            long dias = (minutos + minutosPorDia - 1) / minutosPorDia;
            if (dias <= 0) {
                dias = 1;
            }

            var deposito = llegada.getDepositoDestino();
            BigDecimal tarifaDia = deposito.getTarifaEstadiaDia();

            if (tarifaDia != null) {
                BigDecimal costoDepo = tarifaDia.multiply(BigDecimal.valueOf(dias));
                costoTotalEstadias = costoTotalEstadias.add(costoDepo);
            }
        }
    }

    return costoTotalEstadias;
}

private Long calcularTiempoRealMinutos(List<Tramo> tramos) {
    LocalDateTime minInicio = null;
    LocalDateTime maxFin    = null;

    for (Tramo t : tramos) {
        if (t.getFechaHoraInicio() != null) {
            if (minInicio == null || t.getFechaHoraInicio().isBefore(minInicio)) {
                minInicio = t.getFechaHoraInicio();
            }
        }
        if (t.getFechaHoraFin() != null) {
            if (maxFin == null || t.getFechaHoraFin().isAfter(maxFin)) {
                maxFin = t.getFechaHoraFin();
            }
        }
    }

    if (minInicio == null || maxFin == null) {
        return null; // no se puede calcular
    }

    long minutos = java.time.Duration.between(minInicio, maxFin).toMinutes();
    return Math.max(minutos, 0L);
}

private void actualizarSolicitudComoFinalizada(
        Long idSolicitud,
        BigDecimal costoFinal,
        Long tiempoRealMin
) {
    String url = solicitudBaseUrl + "/" + idSolicitud + "/finalizar";

    FinalizarSolicitudDTO dto = new FinalizarSolicitudDTO();
    dto.setCostoFinal(costoFinal);
    dto.setTiempoReal(tiempoRealMin);
    dto.setEstado("COMPLETADA");

    try {
        restTemplate.postForEntity(url, dto, Void.class);
    } catch (Exception e) {
        System.err.println("No se pudo finalizar la solicitud "
                + idSolicitud + ": " + e.getMessage());
    }
}


private void marcarSolicitudEnTransito(Ruta ruta) {
    if (ruta == null || ruta.getIdSolicitud() == null) {
        return;
    }

    Long idSolicitud = ruta.getIdSolicitud();
    String url = solicitudBaseUrl + "/" + idSolicitud + "/estado"; 
    // solicitudBaseUrl = "http://servicio-solicitud:8082/solicitudes"

    Map<String, Object> body = new HashMap<>();
    body.put("estado", "EN_TRANSITO");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

    try {
        System.out.println(">>> Marcando solicitud " + idSolicitud + " como EN_TRANSITO");
        restTemplate.postForEntity(url, entity, Void.class);
    } catch (Exception e) {
        System.err.println("No se pudo marcar solicitud " + idSolicitud +
                " como EN_TRANSITO: " + e.getMessage());
    }
}

private BigDecimal calcularPrecioBaseContenedor(ContenedorDTO contenedor) {
    if (contenedor == null || contenedor.getVolumen() == null) {
        return BigDecimal.ZERO;
    }

    BigDecimal vol = contenedor.getVolumen();

    // 🚨 Ajustá estos números a lo que diga el enunciado
    if (vol.compareTo(new BigDecimal("30")) <= 0) {
        return new BigDecimal("100000");  // ejemplo chico
    } else if (vol.compareTo(new BigDecimal("60")) <= 0) {
        return new BigDecimal("180000");  // ejemplo mediano
    } else {
        return new BigDecimal("250000");  // ejemplo grande
    }
}

private ContenedorDTO obtenerContenedorDeSolicitud(Long idSolicitud) {
    try {
        // 1) Traer la solicitud
        String urlSolicitud = solicitudBaseUrl + "/" + idSolicitud;
        ResponseEntity<SolicitudDTO> respSol =
                restTemplate.getForEntity(urlSolicitud, SolicitudDTO.class);

        if (!respSol.getStatusCode().is2xxSuccessful() || respSol.getBody() == null) {
            return null;
        }

        Long idCont = respSol.getBody().getIdContenedor();
        if (idCont == null) {
            return null;
        }

        // 2) Traer el contenedor
        String urlCont = contenedorBaseUrl + "/" + idCont;
        ResponseEntity<ContenedorDTO> respCont =
                restTemplate.getForEntity(urlCont, ContenedorDTO.class);

        if (!respCont.getStatusCode().is2xxSuccessful()) {
            return null;
        }

        return respCont.getBody();

    } catch (Exception e) {
        System.err.println("No se pudo obtener contenedor para la solicitud "
                + idSolicitud + ": " + e.getMessage());
        return null;
    }
}


}

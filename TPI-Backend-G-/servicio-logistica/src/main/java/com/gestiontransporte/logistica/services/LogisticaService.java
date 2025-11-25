package com.gestiontransporte.logistica.services;

import com.gestiontransporte.logistica.dto.CamionDTO;
import com.gestiontransporte.logistica.dto.ContenedorDTO;
import com.gestiontransporte.logistica.dto.CrearRutaRequestDTO;
import com.gestiontransporte.logistica.dto.FinalizarSolicitudDTO;
import com.gestiontransporte.logistica.dto.OsrmResponseDTO;
import com.gestiontransporte.logistica.dto.OsrmRouteDTO;
import com.gestiontransporte.logistica.dto.PuntoRutaDTO;
import com.gestiontransporte.logistica.dto.RutaAlternativaDTO;
import com.gestiontransporte.logistica.dto.RutasAlternativasRequestDTO;
import com.gestiontransporte.logistica.dto.SolicitudDTO;
import com.gestiontransporte.logistica.dto.TramoAlternativoDTO;
import com.gestiontransporte.logistica.models.Deposito;
import com.gestiontransporte.logistica.models.EstadoTramo;
import com.gestiontransporte.logistica.models.Localizacion;
import com.gestiontransporte.logistica.models.Ruta;
import com.gestiontransporte.logistica.models.Tramo;
import com.gestiontransporte.logistica.repositories.DepositoRepository;
import com.gestiontransporte.logistica.repositories.LocalizacionRepository;
import com.gestiontransporte.logistica.repositories.RutaRepository;
import com.gestiontransporte.logistica.repositories.TramoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LogisticaService {

    private final RutaRepository rutaRepository;
    private final TramoRepository tramoRepository;
    private final DepositoRepository depositoRepository;
    private final ApiClientService apiClientService;
    private final LocalizacionRepository localizacionRepository;

    public LogisticaService(
            RutaRepository rutaRepository,
            TramoRepository tramoRepository,
            DepositoRepository depositoRepository,
            ApiClientService apiClientService,
            LocalizacionRepository localizacionRepository
    ) {
        this.rutaRepository = rutaRepository;
        this.tramoRepository = tramoRepository;
        this.depositoRepository = depositoRepository;
        this.apiClientService = apiClientService;
        this.localizacionRepository = localizacionRepository;
    }


    public Tramo cambiarEstadoTramo(Long idTramo, String nuevoEstadoRaw, BigDecimal costoReal) {
        Tramo tramo = tramoRepository.findById(idTramo)
                .orElseThrow(() -> new EntityNotFoundException("No existe tramo con id " + idTramo));

        EstadoTramo nuevoEstado;
        try {
            nuevoEstado = EstadoTramo.valueOf(nuevoEstadoRaw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de tramo no soportado: " + nuevoEstadoRaw);
        }

        switch (nuevoEstado) {
            case EN_TRASLADO:

                if (tramo.getPatenteCamion() == null) {
                    throw new IllegalStateException("El tramo no tiene camión asignado.");
                }

                CamionDTO camion = apiClientService.getCamion(tramo.getPatenteCamion());
                if (camion == null) {
                    throw new IllegalStateException("No se pudo obtener el camión desde servicio-transporte.");
                }

                if (Boolean.FALSE.equals(camion.getDisponibilidad())) {
                    throw new IllegalStateException(
                            "El camión " + camion.getPatente() + " no está disponible para iniciar este tramo."
                    );
                }

                if (tramo.getFechaHoraInicio() == null) {
                    tramo.setFechaHoraInicio(LocalDateTime.now());
                }

                tramo.setEstado(EstadoTramo.EN_TRASLADO);

                apiClientService.actualizarDisponibilidadCamion(tramo.getPatenteCamion(), false);

                break;

            case FINALIZADO:

                if (tramo.getFechaHoraInicio() == null) {
                    throw new IllegalStateException("No se puede finalizar un tramo que nunca inició");
                }

                if (tramo.getFechaHoraFin() == null) {
                    tramo.setFechaHoraFin(LocalDateTime.now());
                }

                tramo.setEstado(EstadoTramo.FINALIZADO);

                BigDecimal costoFinal;

                if (costoReal != null) {
                    costoFinal = costoReal;
                } else {
                    CamionDTO camionDTO = apiClientService.getCamion(tramo.getPatenteCamion());

                    BigDecimal consumoReal = camionDTO.getConsumoRealLitrosKm();
                    BigDecimal precioGasoil = apiClientService.getPrecioGasoil();

                    costoFinal = tramo.getDistanciaKm()
                            .multiply(consumoReal)
                            .multiply(precioGasoil)
                            .setScale(2, RoundingMode.HALF_UP);
                }

                tramo.setCostoReal(costoFinal);

                apiClientService.actualizarDisponibilidadCamion(tramo.getPatenteCamion(), true);
                break;

            default:
                throw new IllegalArgumentException("Estado de tramo no soportado: " + nuevoEstadoRaw);
        }

        tramo = tramoRepository.save(tramo);

        if (tramo.getEstado() == EstadoTramo.EN_TRASLADO) {
            marcarSolicitudEnTransito(tramo.getRuta());
        }

        if (tramo.getEstado() == EstadoTramo.FINALIZADO) {
            procesarSiRutaFinalizada(tramo.getRuta());
        }

        return tramo;
    }

    public Ruta crearRutaParaSolicitud(CrearRutaRequestDTO request) {

        Long idSolicitud = request.getIdSolicitud();

        // existe solicitud 
        if (!apiClientService.existeSolicitud(idSolicitud)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La solicitud " + idSolicitud + " no existe en servicio-solicitud"
            );
        }

        // get de la solicitud completa
        SolicitudDTO solicitudDTO = apiClientService.getSolicitud(idSolicitud);
        if (solicitudDTO == null ||
                solicitudDTO.getOrigen() == null ||
                solicitudDTO.getDestino() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La solicitud " + idSolicitud + " no tiene origen/destino cargados"
            );
        }

        // Armar los PuntoRutaDTO para OSRM
        PuntoRutaDTO origenDTO = new PuntoRutaDTO();
        origenDTO.setLatitud(solicitudDTO.getOrigen().getLatitud());
        origenDTO.setLongitud(solicitudDTO.getOrigen().getLongitud());

        PuntoRutaDTO destinoDTO = new PuntoRutaDTO();
        destinoDTO.setLatitud(solicitudDTO.getDestino().getLatitud());
        destinoDTO.setLongitud(solicitudDTO.getDestino().getLongitud());

        // Depósitos intermedios
        List<Deposito> depositosEnRuta = new ArrayList<>();

        if (request.getPuntosIntermedios() != null) {
            for (PuntoRutaDTO p : request.getPuntosIntermedios()) {
                if (p.getIdDeposito() == null) {
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
        int cantidadTramos = (cantidadDepositos == 0) ? 1 : (cantidadDepositos + 1);

        // existe Ruta para esa solicitud
        Ruta ruta = rutaRepository.findByIdSolicitud(idSolicitud).orElse(null);

        if (ruta != null) {
            tramoRepository.deleteByRuta(ruta);
        } else {
            ruta = new Ruta();
            ruta.setIdSolicitud(idSolicitud);
        }

        // Seteo Localizacion
        Localizacion origenLoc = localizacionRepository.findById(
                solicitudDTO.getOrigen().getIdLocalizacion()
        ).orElseThrow(() -> new RuntimeException("Origen no existe"));

        Localizacion destinoLoc = localizacionRepository.findById(
                solicitudDTO.getDestino().getIdLocalizacion()
        ).orElseThrow(() -> new RuntimeException("Destino no existe"));

        ruta.setOrigen(origenLoc);
        ruta.setDestino(destinoLoc);
        ruta.setCantidadDepositos(cantidadDepositos);
        ruta.setCantidadTramos(cantidadTramos);

        ruta = rutaRepository.save(ruta);

        // Calculos
        BigDecimal costoTotalEstimado = BigDecimal.ZERO;
        BigDecimal tiempoTotalMin = BigDecimal.ZERO;

        if (depositosEnRuta.isEmpty()) {

            OsrmRouteDTO rutaOsrm = consultarDistanciaOsrm(
                    origenDTO.getLatitud(),
                    origenDTO.getLongitud(),
                    destinoDTO.getLatitud(),
                    destinoDTO.getLongitud()
            );

            BigDecimal distanciaKm = BigDecimal
                    .valueOf(rutaOsrm.getDistance() / 1000.0)
                    .setScale(3, RoundingMode.HALF_UP);

            BigDecimal duracionMin = BigDecimal
                    .valueOf(rutaOsrm.getDuration() / 60.0)
                    .setScale(1, RoundingMode.HALF_UP);

            BigDecimal costoTramo = calcularCostoAproximado(distanciaKm);

            costoTotalEstimado = costoTotalEstimado.add(costoTramo);
            tiempoTotalMin = tiempoTotalMin.add(duracionMin);

            Tramo tramo = crearTramo(
                    ruta,
                    null,
                    null,
                    "origen-destino",
                    solicitudDTO.getOrigen().getDescripcion(),
                    solicitudDTO.getDestino().getDescripcion(),
                    distanciaKm,
                    duracionMin,
                    costoTramo
            );

            tramoRepository.save(tramo);

        } else {
            // ORIGEN -> PRIMER DEPÓSITO
            Deposito primerDepo = depositosEnRuta.get(0);

            OsrmRouteDTO rutaOsrm1 = consultarDistanciaOsrm(
                    origenDTO.getLatitud(),
                    origenDTO.getLongitud(),
                    primerDepo.getLatitud(),
                    primerDepo.getLongitud()
            );

            BigDecimal distKm1 = BigDecimal
                    .valueOf(rutaOsrm1.getDistance() / 1000.0)
                    .setScale(3, RoundingMode.HALF_UP);

            BigDecimal durMin1 = BigDecimal
                    .valueOf(rutaOsrm1.getDuration() / 60.0)
                    .setScale(1, RoundingMode.HALF_UP);

            BigDecimal costoTramo1 = calcularCostoAproximado(distKm1);

            costoTotalEstimado = costoTotalEstimado.add(costoTramo1);
            tiempoTotalMin = tiempoTotalMin.add(durMin1);

            Tramo tramo1 = crearTramo(
                    ruta,
                    null,
                    primerDepo,
                    "origen-deposito",
                    solicitudDTO.getOrigen().getDescripcion(),
                    primerDepo.getNombre(),
                    distKm1,
                    durMin1,
                    costoTramo1
            );
            tramoRepository.save(tramo1);

            // DEPÓSITO → DEPÓSITO
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

                BigDecimal costoTramo = calcularCostoAproximado(distanciaKm);

                costoTotalEstimado = costoTotalEstimado.add(costoTramo);
                tiempoTotalMin = tiempoTotalMin.add(duracionMin);

                Tramo tramo = crearTramo(
                        ruta,
                        depOrigen,
                        depDestino,
                        "deposito-deposito",
                        depOrigen.getNombre(),
                        depDestino.getNombre(),
                        distanciaKm,
                        duracionMin,
                        costoTramo
                );
                tramoRepository.save(tramo);
            }

            // ÚLTIMO DEPÓSITO → DESTINO
            Deposito ultimoDepo = depositosEnRuta.get(depositosEnRuta.size() - 1);

            OsrmRouteDTO rutaOsrmLast = consultarDistanciaOsrm(
                    ultimoDepo.getLatitud(),
                    ultimoDepo.getLongitud(),
                    destinoDTO.getLatitud(),
                    destinoDTO.getLongitud()
            );

            BigDecimal distKmLast = BigDecimal
                    .valueOf(rutaOsrmLast.getDistance() / 1000.0)
                    .setScale(3, RoundingMode.HALF_UP);

            BigDecimal durMinLast = BigDecimal
                    .valueOf(rutaOsrmLast.getDuration() / 60.0)
                    .setScale(1, RoundingMode.HALF_UP);

            BigDecimal costoTramoLast = calcularCostoAproximado(distKmLast);

            costoTotalEstimado = costoTotalEstimado.add(costoTramoLast);
            tiempoTotalMin = tiempoTotalMin.add(durMinLast);

            Tramo tramoLast = crearTramo(
                    ruta,
                    ultimoDepo,
                    null,
                    "deposito-destino",
                    ultimoDepo.getNombre(),
                    solicitudDTO.getDestino().getDescripcion(),
                    distKmLast,
                    durMinLast,
                    costoTramoLast
            );
            tramoRepository.save(tramoLast);
        }

        // Tarifa base contenedor
        ContenedorDTO contenedorDTO = obtenerContenedorDeSolicitud(idSolicitud);
        BigDecimal tarifaBaseContenedor = calcularPrecioBaseContenedor(contenedorDTO);

        costoTotalEstimado = costoTotalEstimado.add(tarifaBaseContenedor);

        // Actualizar estimación solicitud
        apiClientService.actualizarEstimacionSolicitud(
                idSolicitud,
                costoTotalEstimado,
                tiempoTotalMin.longValue()
        );

        // cargo tramos y devuelvo la ruta que contiene los tramos
        List<Tramo> tramosRuta = tramoRepository.findByRutaOrderByIdTramoAsc(ruta);
        ruta.setTramos(tramosRuta);

        return ruta;
    }

    //helper
    private Tramo crearTramo(
            Ruta ruta,
            Deposito depositoOrigen,
            Deposito depositoDestino,
            String tipo,
            String descripcionOrigen,
            String descripcionDestino,
            BigDecimal distanciaKm,
            BigDecimal duracionMin,
            BigDecimal costoTramo
    ) {
        Tramo tramo = new Tramo();
        tramo.setRuta(ruta);
        tramo.setDepositoOrigen(depositoOrigen);
        tramo.setDepositoDestino(depositoDestino);
        tramo.setTipo(tipo);

        tramo.setEstado(EstadoTramo.ESTIMADO);
        tramo.setCostoAproximado(costoTramo);
        tramo.setCostoReal(null);
        tramo.setPatenteCamion(null);

        tramo.setTiempoEstimado(duracionMin);
        tramo.setDescripcionOrigen(descripcionOrigen);
        tramo.setDescripcionDestino(descripcionDestino);
        tramo.setDistanciaKm(distanciaKm);

        return tramo;
    }
    // OSRM
    private OsrmRouteDTO consultarDistanciaOsrm(
            BigDecimal latitudOrigen,
            BigDecimal longitudOrigen,
            BigDecimal latitudDestino,
            BigDecimal longitudDestino
    ) {
        String coords = String.format(
                "%s,%s;%s,%s",
                longitudOrigen.toPlainString(),
                latitudOrigen.toPlainString(),
                longitudDestino.toPlainString(),
                latitudDestino.toPlainString()
        );

        OsrmResponseDTO response = apiClientService.getDistancia(coords);

        if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()) {
            throw new IllegalStateException("No se pudo obtener ruta desde OSRM");
        }

        return response.getRoutes().get(0);
    }

    // alternativas
    private RutaAlternativaDTO calcularRutaAlternativa(
            PuntoRutaDTO origenDTO,
            PuntoRutaDTO destinoDTO,
            List<Deposito> depositosEnRuta,
            String nombreAlternativa,
            String descripcionOrigen,
            String descripcionDestino
    ) {
        BigDecimal costoTotalEstimado = BigDecimal.ZERO;
        BigDecimal tiempoTotalMin = BigDecimal.ZERO;

        List<String> descripcionTramos = new ArrayList<>();
        List<TramoAlternativoDTO> tramosDetalle = new ArrayList<>();

        // SIN DEPÓSITOS
        if (depositosEnRuta == null || depositosEnRuta.isEmpty()) {

            OsrmRouteDTO rutaOsrm = consultarDistanciaOsrm(
                    origenDTO.getLatitud(),
                    origenDTO.getLongitud(),
                    destinoDTO.getLatitud(),
                    destinoDTO.getLongitud()
            );

            BigDecimal distanciaKm = BigDecimal
                    .valueOf(rutaOsrm.getDistance() / 1000.0)
                    .setScale(3, RoundingMode.HALF_UP);

            BigDecimal duracionMin = BigDecimal
                    .valueOf(rutaOsrm.getDuration() / 60.0)
                    .setScale(1, RoundingMode.HALF_UP);

            BigDecimal costoTramo = calcularCostoAproximado(distanciaKm);

            costoTotalEstimado = costoTotalEstimado.add(costoTramo);
            tiempoTotalMin = tiempoTotalMin.add(duracionMin);

            descripcionTramos.add(
                    "Origen (" + descripcionOrigen + ") → Destino (" + descripcionDestino + ")"
            );

            TramoAlternativoDTO tramo = new TramoAlternativoDTO();
            tramo.setTipo("origen-destino");
            tramo.setDescripcionOrigen(descripcionOrigen);
            tramo.setDescripcionDestino(descripcionDestino);
            tramo.setLatitudOrigen(origenDTO.getLatitud());
            tramo.setLongitudOrigen(origenDTO.getLongitud());
            tramo.setLatitudDestino(destinoDTO.getLatitud());
            tramo.setLongitudDestino(destinoDTO.getLongitud());
            tramo.setDistanciaKm(distanciaKm);
            tramo.setTiempoMin(duracionMin);

            tramosDetalle.add(tramo);

            RutaAlternativaDTO dto = new RutaAlternativaDTO();
            dto.setNombre(nombreAlternativa);
            dto.setCostoEstimado(costoTotalEstimado);
            dto.setTiempoEstimadoMin(tiempoTotalMin);
            dto.setCantidadDepositos(0);
            dto.setCantidadTramos(1);
            dto.setDescripcionTramos(descripcionTramos);
            dto.setTramos(tramosDetalle);

            return dto;
        }

        // CON DEPÓSITOS

        // ORIGEN → PRIMER DEPÓSITO
        Deposito primerDepo = depositosEnRuta.get(0);

        OsrmRouteDTO rutaOsrm1 = consultarDistanciaOsrm(
                origenDTO.getLatitud(),
                origenDTO.getLongitud(),
                primerDepo.getLatitud(),
                primerDepo.getLongitud()
        );

        BigDecimal distKm1 = BigDecimal.valueOf(rutaOsrm1.getDistance() / 1000.0).setScale(3, RoundingMode.HALF_UP);
        BigDecimal durMin1 = BigDecimal.valueOf(rutaOsrm1.getDuration() / 60.0).setScale(1, RoundingMode.HALF_UP);
        BigDecimal costoTramo1 = calcularCostoAproximado(distKm1);

        costoTotalEstimado = costoTotalEstimado.add(costoTramo1);
        tiempoTotalMin = tiempoTotalMin.add(durMin1);

        descripcionTramos.add("Origen (" + descripcionOrigen + ") → Depósito " + primerDepo.getNombre());

        TramoAlternativoDTO tramo1 = new TramoAlternativoDTO();
        tramo1.setTipo("origen-deposito");
        tramo1.setDescripcionOrigen(descripcionOrigen);
        tramo1.setDescripcionDestino(primerDepo.getNombre());
        tramo1.setLatitudOrigen(origenDTO.getLatitud());
        tramo1.setLongitudOrigen(origenDTO.getLongitud());
        tramo1.setLatitudDestino(primerDepo.getLatitud());
        tramo1.setLongitudDestino(primerDepo.getLongitud());
        tramo1.setDistanciaKm(distKm1);
        tramo1.setTiempoMin(durMin1);
        tramosDetalle.add(tramo1);

        // DEPÓSITO → DEPÓSITO
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

            BigDecimal costoTramo = calcularCostoAproximado(distanciaKm);

            costoTotalEstimado = costoTotalEstimado.add(costoTramo);
            tiempoTotalMin = tiempoTotalMin.add(duracionMin);

            descripcionTramos.add(
                    "Depósito " + depOrigen.getNombre() + " → Depósito " + depDestino.getNombre()
            );

            TramoAlternativoDTO tramo = new TramoAlternativoDTO();
            tramo.setTipo("deposito-deposito");
            tramo.setDescripcionOrigen(depOrigen.getNombre());
            tramo.setDescripcionDestino(depDestino.getNombre());
            tramo.setLatitudOrigen(depOrigen.getLatitud());
            tramo.setLongitudOrigen(depOrigen.getLongitud());
            tramo.setLatitudDestino(depDestino.getLatitud());
            tramo.setLongitudDestino(depDestino.getLongitud());
            tramo.setDistanciaKm(distanciaKm);
            tramo.setTiempoMin(duracionMin);
            tramosDetalle.add(tramo);
        }

        // ÚLTIMO DEPÓSITO → DESTINO
        Deposito ultimoDepo = depositosEnRuta.get(depositosEnRuta.size() - 1);

        OsrmRouteDTO rutaOsrmLast = consultarDistanciaOsrm(
                ultimoDepo.getLatitud(),
                ultimoDepo.getLongitud(),
                destinoDTO.getLatitud(),
                destinoDTO.getLongitud()
        );

        BigDecimal distKmLast = BigDecimal.valueOf(rutaOsrmLast.getDistance() / 1000.0).setScale(3, RoundingMode.HALF_UP);
        BigDecimal durMinLast = BigDecimal.valueOf(rutaOsrmLast.getDuration() / 60.0).setScale(1, RoundingMode.HALF_UP);
        BigDecimal costoTramoLast = calcularCostoAproximado(distKmLast);

        costoTotalEstimado = costoTotalEstimado.add(costoTramoLast);
        tiempoTotalMin = tiempoTotalMin.add(durMinLast);

        descripcionTramos.add(
                "Depósito " + ultimoDepo.getNombre() + " → Destino (" + descripcionDestino + ")"
        );

        TramoAlternativoDTO tramoLast = new TramoAlternativoDTO();
        tramoLast.setTipo("deposito-destino");
        tramoLast.setDescripcionOrigen(ultimoDepo.getNombre());
        tramoLast.setDescripcionDestino(descripcionDestino);
        tramoLast.setLatitudOrigen(ultimoDepo.getLatitud());
        tramoLast.setLongitudOrigen(ultimoDepo.getLongitud());
        tramoLast.setLatitudDestino(destinoDTO.getLatitud());
        tramoLast.setLongitudDestino(destinoDTO.getLongitud());
        tramoLast.setDistanciaKm(distKmLast);
        tramoLast.setTiempoMin(durMinLast);
        tramosDetalle.add(tramoLast);

        RutaAlternativaDTO dto = new RutaAlternativaDTO();
        dto.setNombre(nombreAlternativa);
        dto.setCostoEstimado(costoTotalEstimado);
        dto.setTiempoEstimadoMin(tiempoTotalMin);
        dto.setCantidadDepositos(depositosEnRuta.size());
        dto.setCantidadTramos(depositosEnRuta.size() + 1);
        dto.setDescripcionTramos(descripcionTramos);
        dto.setTramos(tramosDetalle);

        return dto;
    }

    public List<RutaAlternativaDTO> generarRutasAlternativas(RutasAlternativasRequestDTO request) {

        Long idSolicitud = request.getIdSolicitud();

        if (!apiClientService.existeSolicitud(idSolicitud)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La solicitud " + idSolicitud + " no existe en servicio-solicitud"
            );
        }

        SolicitudDTO solicitudDTO = apiClientService.getSolicitud(idSolicitud);
        if (solicitudDTO == null ||
                solicitudDTO.getOrigen() == null ||
                solicitudDTO.getDestino() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La solicitud " + idSolicitud + " no tiene origen/destino cargados"
            );
        }

        PuntoRutaDTO origenDTO = new PuntoRutaDTO();
        origenDTO.setLatitud(solicitudDTO.getOrigen().getLatitud());
        origenDTO.setLongitud(solicitudDTO.getOrigen().getLongitud());

        PuntoRutaDTO destinoDTO = new PuntoRutaDTO();
        destinoDTO.setLatitud(solicitudDTO.getDestino().getLatitud());
        destinoDTO.setLongitud(solicitudDTO.getDestino().getLongitud());

        String descripcionOrigen = solicitudDTO.getOrigen().getDescripcion();
        String descripcionDestino = solicitudDTO.getDestino().getDescripcion();

        List<RutaAlternativaDTO> alternativas = new ArrayList<>();

        // Ruta directa
        RutaAlternativaDTO directa = calcularRutaAlternativa(
                origenDTO,
                destinoDTO,
                new ArrayList<>(),
                "Ruta directa",
                descripcionOrigen,
                descripcionDestino
        );
        alternativas.add(directa);

        // Rutas con depósitos
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
                        nombre,
                        descripcionOrigen,
                        descripcionDestino
                );

                alternativas.add(alternativa);
            }
        }

        alternativas.sort(Comparator.comparing(RutaAlternativaDTO::getCostoEstimado));

        return alternativas;
    }

    // costoAprox
    private BigDecimal calcularCostoAproximado(BigDecimal distanciaKm) {

        BigDecimal consumoPromedio = new BigDecimal("0.35");

        BigDecimal precioGasoil = apiClientService.getPrecioGasoil();

        return distanciaKm
                .multiply(consumoPromedio)
                .multiply(precioGasoil)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // asignacion camion
    public Tramo asignarCamionATramo(Long idTramo, String patenteCamion) {
        Tramo tramo = tramoRepository.findById(idTramo)
                .orElseThrow(() -> new IllegalArgumentException("Tramo " + idTramo + " no existe."));

        CamionDTO camionDTO = apiClientService.getCamion(patenteCamion);
        if (camionDTO == null) {
            throw new IllegalArgumentException("El camión con patente " + patenteCamion + " no existe.");
        }

        if (Boolean.FALSE.equals(camionDTO.getDisponibilidad())) {
            throw new IllegalStateException("El camión " + patenteCamion + " no está disponible.");
        }

        Ruta ruta = tramo.getRuta();
        if (ruta == null || ruta.getIdSolicitud() == null) {
            throw new IllegalStateException("El tramo no tiene ruta/solicitud asociada.");
        }

        Long idSolicitud = ruta.getIdSolicitud();

        SolicitudDTO solicitudDTO = apiClientService.getSolicitud(idSolicitud);
        if (solicitudDTO == null) {
            throw new IllegalStateException("No se pudo obtener la solicitud " + idSolicitud
                    + " desde servicio-solicitud.");
        }

        if (solicitudDTO.getIdContenedor() == null) {
            throw new IllegalStateException("La solicitud " + idSolicitud + " no tiene contenedor asociado.");
        }

        Long idContenedor = solicitudDTO.getIdContenedor();

        ContenedorDTO contenedorDTO = apiClientService.getContenedor(idContenedor);
        if (contenedorDTO == null) {
            throw new IllegalStateException("No se pudo obtener el contenedor " + idContenedor
                    + " desde servicio-contenedor.");
        }

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

        tramo.setPatenteCamion(patenteCamion);
        tramo.setEstado(EstadoTramo.ASIGNADO);

        return tramoRepository.save(tramo);
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
                .allMatch(t -> t.getEstado() == EstadoTramo.FINALIZADO);

        System.out.println(">>> todosFinalizados = " + todosFinalizados);
        if (!todosFinalizados) {
            return;
        }

        cerrarSolicitudConCostosYTiempo(ruta, tramos);
    }

    private void cerrarSolicitudConCostosYTiempo(Ruta ruta, List<Tramo> tramos) {
        Long idSolicitud = ruta.getIdSolicitud();
        if (idSolicitud == null) {
            System.err.println(">>> La ruta " + ruta.getIdRuta() + " no tiene idSolicitud asociado");
            return;
        }

        BigDecimal costoTramos = BigDecimal.ZERO;
        for (Tramo t : tramos) {
            if (t.getCostoReal() != null) {
                costoTramos = costoTramos.add(t.getCostoReal());
            } else if (t.getCostoAproximado() != null) {
                costoTramos = costoTramos.add(t.getCostoAproximado());
            }
        }

        BigDecimal costoEstadias = calcularCostoEstadiasPorDia(tramos);

        ContenedorDTO contenedorDTO = null;
        try {
            SolicitudDTO solicitud = apiClientService.getSolicitud(idSolicitud);
            if (solicitud != null && solicitud.getIdContenedor() != null) {
                Long idCont = solicitud.getIdContenedor();
                contenedorDTO = apiClientService.getContenedor(idCont);
            }
        } catch (Exception e) {
            System.err.println("No se pudo obtener contenedor para la solicitud "
                    + idSolicitud + ": " + e.getMessage());
        }

        BigDecimal tarifaBaseContenedor = calcularPrecioBaseContenedor(contenedorDTO);

        BigDecimal costoFinal = costoTramos
                .add(costoEstadias)
                .add(tarifaBaseContenedor);

        Long tiempoRealMin = calcularTiempoRealMinutos(tramos);

        actualizarSolicitudComoFinalizada(idSolicitud, costoFinal, tiempoRealMin);
    }

    private BigDecimal calcularCostoEstadiasPorDia(List<Tramo> tramos) {
        BigDecimal costoTotalEstadias = BigDecimal.ZERO;
        long minutosPorDia = 60L * 24L;

        for (int i = 0; i < tramos.size() - 1; i++) {
            Tramo llegada = tramos.get(i);
            Tramo salida = tramos.get(i + 1);

            if (llegada.getDepositoDestino() != null
                    && salida.getDepositoOrigen() != null
                    && llegada.getDepositoDestino().getIdDeposito()
                    .equals(salida.getDepositoOrigen().getIdDeposito())) {

                if (llegada.getFechaHoraFin() == null || salida.getFechaHoraInicio() == null) {
                    continue;
                }

                long minutos = java.time.Duration.between(
                        llegada.getFechaHoraFin(),
                        salida.getFechaHoraInicio()
                ).toMinutes();

                if (minutos <= 0) {
                    continue;
                }

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
        LocalDateTime maxFin = null;

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
            return null;
        }

        long minutos = java.time.Duration.between(minInicio, maxFin).toMinutes();
        return Math.max(minutos, 0L);
    }

    private void actualizarSolicitudComoFinalizada(
            Long idSolicitud,
            BigDecimal costoFinal,
            Long tiempoRealMin
    ) {
        FinalizarSolicitudDTO dto = new FinalizarSolicitudDTO();
        dto.setCostoFinal(costoFinal);
        dto.setTiempoReal(tiempoRealMin);
        dto.setEstado("COMPLETADA");

        apiClientService.finalizarSolicitud(idSolicitud, dto);
    }

    private void marcarSolicitudEnTransito(Ruta ruta) {
        if (ruta == null || ruta.getIdSolicitud() == null) {
            return;
        }
        apiClientService.actualizarEstadoSolicitudEnTransito(ruta.getIdSolicitud());
    }

    private BigDecimal calcularPrecioBaseContenedor(ContenedorDTO contenedor) {
        if (contenedor == null || contenedor.getVolumen() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal vol = contenedor.getVolumen();

        if (vol.compareTo(new BigDecimal("30")) <= 0) {
            return new BigDecimal("100000");
        } else if (vol.compareTo(new BigDecimal("60")) <= 0) {
            return new BigDecimal("180000");
        } else {
            return new BigDecimal("250000");
        }
    }

    private ContenedorDTO obtenerContenedorDeSolicitud(Long idSolicitud) {
        try {
            SolicitudDTO solicitud = apiClientService.getSolicitud(idSolicitud);
            if (solicitud == null || solicitud.getIdContenedor() == null) {
                return null;
            }

            return apiClientService.getContenedor(solicitud.getIdContenedor());
        } catch (Exception e) {
            System.err.println("No se pudo obtener contenedor para la solicitud "
                    + idSolicitud + ": " + e.getMessage());
            return null;
        }
    }
}

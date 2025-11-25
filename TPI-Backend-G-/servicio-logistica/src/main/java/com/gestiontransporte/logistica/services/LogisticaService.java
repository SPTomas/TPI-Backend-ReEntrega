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
    // =========================
    // CAMBIAR ESTADO TRAMO
    // =========================
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

                // ⚠️ Traemos el camión desde transporte
                CamionDTO camion = apiClientService.getCamion(tramo.getPatenteCamion());
                if (camion == null) {
                    throw new IllegalStateException("No se pudo obtener el camión desde servicio-transporte.");
                }

                // Si no está disponible → NO PERMITIR INICIO
                if (Boolean.FALSE.equals(camion.getDisponibilidad())) {
                    throw new IllegalStateException(
                        "El camión " + camion.getPatente() + " no está disponible para iniciar este tramo."
                    );
                }

                // ✔ Si está disponible, iniciar tramo
                if (tramo.getFechaHoraInicio() == null) {
                    tramo.setFechaHoraInicio(LocalDateTime.now());
                }

                tramo.setEstado(EstadoTramo.EN_TRASLADO);

                // 🔒 Bloquear el camión
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

            // si el operador manda un costo manual, usarlo
            if (costoReal != null) {
                costoFinal = costoReal;
            } 
            else {
                // consumo REAL del camión
                CamionDTO camionDTO = apiClientService.getCamion(tramo.getPatenteCamion());

                BigDecimal consumoReal = camionDTO.getConsumoRealLitrosKm();
                BigDecimal precioGasoil = apiClientService.getPrecioGasoil();

                costoFinal = tramo.getDistanciaKm()
                        .multiply(consumoReal)
                        .multiply(precioGasoil)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            tramo.setCostoReal(costoFinal);

            // devolver disponibilidad
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

    // =========================
    // CREAR RUTA + TRAMOS
    // =========================
public Ruta crearRutaParaSolicitud(CrearRutaRequestDTO request) {

    Long idSolicitud = request.getIdSolicitud();

    // 1) Validar que la solicitud exista
    if (!apiClientService.existeSolicitud(idSolicitud)) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "La solicitud " + idSolicitud + " no existe en servicio-solicitud"
        );
    }

    // 2) Traer la solicitud completa (para origen/destino y contenedor)
    SolicitudDTO solicitudDTO = apiClientService.getSolicitud(idSolicitud);
    if (solicitudDTO == null ||
        solicitudDTO.getOrigen() == null ||
        solicitudDTO.getDestino() == null) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud " + idSolicitud + " no tiene origen/destino cargados"
        );
    }

    // 3) Armar los PuntoRutaDTO para OSRM
        PuntoRutaDTO origenDTO = new PuntoRutaDTO();
        origenDTO.setLatitud(solicitudDTO.getOrigen().getLatitud());
        origenDTO.setLongitud(solicitudDTO.getOrigen().getLongitud());

        PuntoRutaDTO destinoDTO = new PuntoRutaDTO();
        destinoDTO.setLatitud(solicitudDTO.getDestino().getLatitud());
        destinoDTO.setLongitud(solicitudDTO.getDestino().getLongitud());


    // 4) Armar la lista de depósitos en orden (igual que antes)
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

    // 5) Ver si ya existe Ruta para esa solicitud
    Ruta ruta = rutaRepository.findByIdSolicitud(idSolicitud).orElse(null);

    if (ruta != null) {
        tramoRepository.deleteByRuta(ruta);
    } else {
        ruta = new Ruta();
        ruta.setIdSolicitud(idSolicitud);
    }

    // 6) Setear ORIGEN y DESTINO como Localizacion (mismo id que la solicitud)
    //    OJO: acá asumo que tu DTO de localización tiene getIdLocalizacion()
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

    // 7) Calcular tramos + costos/tiempos (igual que ya tenías)

    BigDecimal costoTotalEstimado = BigDecimal.ZERO;
    BigDecimal tiempoTotalMin = BigDecimal.ZERO;

    // CASO A: SIN DEPÓSITOS
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

        BigDecimal costoTramo = calcularCostoAproximado(distanciaKm);

        costoTotalEstimado = costoTotalEstimado.add(costoTramo);
        tiempoTotalMin = tiempoTotalMin.add(duracionMin);

        Tramo tramo = new Tramo();
        tramo.setRuta(ruta);
        tramo.setDepositoOrigen(null);
        tramo.setDepositoDestino(null);
        tramo.setTipo("origen-destino");
        tramo.setEstado(EstadoTramo.ESTIMADO);
        tramo.setCostoAproximado(costoTramo);
        tramo.setCostoReal(null);
        tramo.setPatenteCamion(null);
        tramo.setTiempoEstimado(duracionMin);
        tramo.setDescripcionOrigen(solicitudDTO.getOrigen().getDescripcion());
        tramo.setDescripcionDestino(solicitudDTO.getDestino().getDescripcion());
        tramo.setDistanciaKm(distanciaKm);

        tramoRepository.save(tramo);

    } else {
        // CASO B: CON DEPÓSITOS

        // B1) ORIGEN -> PRIMER DEPÓSITO
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

        BigDecimal costoTramo1 = calcularCostoAproximado(distKm1);

        costoTotalEstimado = costoTotalEstimado.add(costoTramo1);
        tiempoTotalMin = tiempoTotalMin.add(durMin1);

        Tramo tramo1 = new Tramo();
        tramo1.setRuta(ruta);
        tramo1.setDepositoOrigen(null);
        tramo1.setDepositoDestino(primerDepo);
        tramo1.setTipo("origen-deposito");
        tramo1.setEstado(EstadoTramo.ESTIMADO);
        tramo1.setCostoAproximado(costoTramo1);
        tramo1.setCostoReal(null);
        tramo1.setPatenteCamion(null);
        tramo1.setTiempoEstimado(durMin1);
        tramo1.setDescripcionOrigen(solicitudDTO.getOrigen().getDescripcion());
        tramo1.setDescripcionDestino(primerDepo.getNombre());
        tramo1.setDistanciaKm(distKm1);

        tramoRepository.save(tramo1);

        // B2) DEPÓSITO -> DEPÓSITO
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

            BigDecimal costoTramo = calcularCostoAproximado(distanciaKm);

            costoTotalEstimado = costoTotalEstimado.add(costoTramo);
            tiempoTotalMin = tiempoTotalMin.add(duracionMin);

            Tramo tramo = new Tramo();
            tramo.setRuta(ruta);
            tramo.setDepositoOrigen(depOrigen);
            tramo.setDepositoDestino(depDestino);
            tramo.setTipo("deposito-deposito");
            tramo.setEstado(EstadoTramo.ESTIMADO);
            tramo.setCostoAproximado(costoTramo);
            tramo.setCostoReal(null);
            tramo.setPatenteCamion(null);
            tramo.setTiempoEstimado(duracionMin);
            tramo.setDescripcionOrigen(depOrigen.getNombre());
            tramo.setDescripcionDestino(depDestino.getNombre());
            tramo.setDistanciaKm(distanciaKm);
            tramoRepository.save(tramo);
        }

        // B3) ÚLTIMO DEPÓSITO -> DESTINO
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

        BigDecimal costoTramoLast = calcularCostoAproximado(distKmLast);

        costoTotalEstimado = costoTotalEstimado.add(costoTramoLast);
        tiempoTotalMin = tiempoTotalMin.add(durMinLast);

        Tramo tramoLast = new Tramo();
        tramoLast.setRuta(ruta);
        tramoLast.setDepositoOrigen(ultimoDepo);
        tramoLast.setDepositoDestino(null);
        tramoLast.setTipo("deposito-destino");
        tramoLast.setEstado(EstadoTramo.ESTIMADO);
        tramoLast.setCostoAproximado(costoTramoLast);
        tramoLast.setCostoReal(null);
        tramoLast.setPatenteCamion(null);
        tramoLast.setTiempoEstimado(durMinLast);
        tramoLast.setDescripcionOrigen(ultimoDepo.getNombre());
        tramoLast.setDescripcionDestino(solicitudDTO.getDestino().getDescripcion());
        tramoLast.setDistanciaKm(distKmLast);

        tramoRepository.save(tramoLast);
    }

    // 8) Tarifa base contenedor
    ContenedorDTO contenedorDTO = obtenerContenedorDeSolicitud(idSolicitud);
    BigDecimal tarifaBaseContenedor = calcularPrecioBaseContenedor(contenedorDTO);

    costoTotalEstimado = costoTotalEstimado.add(tarifaBaseContenedor);

    // 9) Actualizar estimación en servicio-solicitud
    apiClientService.actualizarEstimacionSolicitud(
            idSolicitud,
            costoTotalEstimado,
            tiempoTotalMin.longValue()
    );

    // 10) Cargar tramos en la entidad para devolverlos
    List<Tramo> tramosRuta = tramoRepository.findByRutaOrderByIdTramoAsc(ruta);
    ruta.setTramos(tramosRuta);

    return ruta;
}

    // =========================
    // OSRM
    // =========================
    private OsrmRouteDTO consultarDistanciaOsrm(
            BigDecimal latitudOrigen,
            BigDecimal longitudOrigen,
            BigDecimal latitudDestino,
            BigDecimal longitudDestino
    ) {
        // OSRM usa LON,LAT
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

    // =========================
    // RUTAS ALTERNATIVAS
    // =========================
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

        // ============================================================
        // SIN DEPÓSITOS
        // ============================================================
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

        // ============================================================
        // CON DEPÓSITOS
        // ============================================================

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

        // ============================================================
        // DTO FINAL
        // ============================================================
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

    // Traer solicitud para obtener origen/destino
    SolicitudDTO solicitudDTO = apiClientService.getSolicitud(idSolicitud);
    if (solicitudDTO == null ||
        solicitudDTO.getOrigen() == null ||
        solicitudDTO.getDestino() == null) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La solicitud " + idSolicitud + " no tiene origen/destino cargados"
        );
    }

    // Origen + destino en DTO
    PuntoRutaDTO origenDTO = new PuntoRutaDTO();
    origenDTO.setLatitud(solicitudDTO.getOrigen().getLatitud());
    origenDTO.setLongitud(solicitudDTO.getOrigen().getLongitud());

    PuntoRutaDTO destinoDTO = new PuntoRutaDTO();
    destinoDTO.setLatitud(solicitudDTO.getDestino().getLatitud());
    destinoDTO.setLongitud(solicitudDTO.getDestino().getLongitud());

    // Las descripciones que vienen de Solicitud
    String descripcionOrigen = solicitudDTO.getOrigen().getDescripcion();
    String descripcionDestino = solicitudDTO.getDestino().getDescripcion();

    List<RutaAlternativaDTO> alternativas = new ArrayList<>();

    // ===========================================================
    // 1) Ruta DIRECTA
    // ===========================================================
    RutaAlternativaDTO directa = calcularRutaAlternativa(
            origenDTO,
            destinoDTO,
            new ArrayList<>(),
            "Ruta directa",
            descripcionOrigen,
            descripcionDestino
    );
    alternativas.add(directa);

    // ===========================================================
    // 2) Rutas con DEPÓSITOS
    // ===========================================================
    if (request.getAlternativasDepositos() != null) {

        for (List<Long> idsDepositos : request.getAlternativasDepositos()) {

            if (idsDepositos == null || idsDepositos.isEmpty()) {
                continue;
            }

            // Cargar cada depósito
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

    // Ordenarlas por costo estimado ascendente
    alternativas.sort(Comparator.comparing(RutaAlternativaDTO::getCostoEstimado));

    return alternativas;
}

    // =========================
    // COSTO APROXIMADO
    // =========================
private BigDecimal calcularCostoAproximado(BigDecimal distanciaKm) {

    // Consumo genérico
    BigDecimal consumoPromedio = new BigDecimal("0.35");

    // Precio del gasoil desde transporte
    BigDecimal precioGasoil = apiClientService.getPrecioGasoil();

    return distanciaKm
            .multiply(consumoPromedio)
            .multiply(precioGasoil)
            .setScale(2, RoundingMode.HALF_UP);
}

    // =========================
    // ASIGNAR CAMIÓN
    // =========================
    public Tramo asignarCamionATramo(Long idTramo, String patenteCamion) {
        Tramo tramo = tramoRepository.findById(idTramo)
                .orElseThrow(() -> new IllegalArgumentException("Tramo " + idTramo + " no existe."));

        // 1) Camión
        CamionDTO camionDTO = apiClientService.getCamion(patenteCamion);
        if (camionDTO == null) {
            throw new IllegalArgumentException("El camión con patente " + patenteCamion + " no existe.");
        }

        if (Boolean.FALSE.equals(camionDTO.getDisponibilidad())) {
            throw new IllegalStateException("El camión " + patenteCamion + " no está disponible.");
        }

        // 2) Solicitud asociada
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

        // 3) Contenedor
        ContenedorDTO contenedorDTO = apiClientService.getContenedor(idContenedor);
        if (contenedorDTO == null) {
            throw new IllegalStateException("No se pudo obtener el contenedor " + idContenedor
                    + " desde servicio-contenedor.");
        }

        // 4) Validaciones
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

        // 5) Asignar
        tramo.setPatenteCamion(patenteCamion);
        tramo.setEstado(EstadoTramo.ASIGNADO);

        return tramoRepository.save(tramo);
    }

    // =========================
    // CIERRE DE RUTA Y COSTOS FINALES
    // =========================
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

        // 1) Costo por tramos
        BigDecimal costoTramos = BigDecimal.ZERO;
        for (Tramo t : tramos) {
            if (t.getCostoReal() != null) {
                costoTramos = costoTramos.add(t.getCostoReal());
            } else if (t.getCostoAproximado() != null) {
                costoTramos = costoTramos.add(t.getCostoAproximado());
            }
        }

        // 2) Costo por estadías
        BigDecimal costoEstadias = calcularCostoEstadiasPorDia(tramos);

        // 3) Contenedor para tarifa base
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

        // 4) Costo final total
        BigDecimal costoFinal = costoTramos
                .add(costoEstadias)
                .add(tarifaBaseContenedor);

        // 5) Tiempo real total
        Long tiempoRealMin = calcularTiempoRealMinutos(tramos);

        // 6) Finalizar solicitud
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

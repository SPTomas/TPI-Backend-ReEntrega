package com.gestiontransporte.logistica.controller;

import com.gestiontransporte.logistica.dto.AsignarCamionRequestDTO;
import com.gestiontransporte.logistica.dto.CambiarEstadoTramoRequestDTO;
import com.gestiontransporte.logistica.dto.CrearRutaRequestDTO;
import com.gestiontransporte.logistica.models.Deposito;
import com.gestiontransporte.logistica.models.EstadoTramo;
import com.gestiontransporte.logistica.models.Ruta;
import com.gestiontransporte.logistica.models.Tramo;
import com.gestiontransporte.logistica.repositories.DepositoRepository; 
import com.gestiontransporte.logistica.repositories.RutaRepository;
import com.gestiontransporte.logistica.repositories.TramoRepository;
import com.gestiontransporte.logistica.services.LogisticaService;
import com.gestiontransporte.logistica.dto.RutaAlternativaDTO;
import com.gestiontransporte.logistica.dto.RutasAlternativasRequestDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/logistica")
@Tag(name = "Gestión de Logística", description = "Endpoints para Rutas, Tramos y Depósitos.")
public class LogisticaController {

    private final LogisticaService logisticaService;
    private final DepositoRepository depositoRepository; 
    private final RutaRepository rutaRepository;
private final TramoRepository tramoRepository;

public LogisticaController(LogisticaService logisticaService,
                           DepositoRepository depositoRepository,
                           RutaRepository rutaRepository,
                           TramoRepository tramoRepository) {
    this.logisticaService = logisticaService;
    this.depositoRepository = depositoRepository;
    this.rutaRepository = rutaRepository;
    this.tramoRepository = tramoRepository;
}

    @PostMapping("/solicitudes/{idSolicitud}/asignar-ruta")
    public ResponseEntity<Ruta> calcularRuta(
            @PathVariable Long idSolicitud,
            @RequestBody CrearRutaRequestDTO request
    ) {
        request.setIdSolicitud(idSolicitud);
        Ruta nuevaRuta = logisticaService.crearRutaParaSolicitud(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRuta);
    }


    @PostMapping("/rutas/alternativas")
    @Operation(
        summary = "Genera rutas alternativas para una solicitud",
        description = "Calcula costos y tiempos de varias rutas (directa y con distintos depósitos) sin persistir en BD"
    )

    public ResponseEntity<List<RutaAlternativaDTO>> generarRutasAlternativas(
            @RequestBody RutasAlternativasRequestDTO request
    ) {
        List<RutaAlternativaDTO> alternativas = logisticaService.generarRutasAlternativas(request);
        return ResponseEntity.ok(alternativas);
    }


    @PostMapping("/tramos/{idTramo}/asignar-camion")
    @Operation(summary = "Asigna un camión a un tramo", description = "Rol: OPERADOR")
    public ResponseEntity<Tramo> asignarCamion(
            @PathVariable Long idTramo, 
            @RequestBody AsignarCamionRequestDTO request) {
                
        Tramo tramoActualizado = logisticaService.asignarCamionATramo(idTramo, request.getPatente());
        return ResponseEntity.ok(tramoActualizado);
    }

    @PatchMapping("/tramos/{idTramo}/estado")
    public ResponseEntity<Tramo> cambiarEstadoTramo(
            @PathVariable Long idTramo,
            @RequestBody CambiarEstadoTramoRequestDTO request
    ) {
        Tramo tramoActualizado = logisticaService.cambiarEstadoTramo(
                idTramo,
                request.getNuevoEstado(),
                request.getCostoReal()  
        );
        return ResponseEntity.ok(tramoActualizado);
    }


    @GetMapping("/tramos")
    @Operation(summary = "Lista todos los tramos", description = "Rol: OPERADOR")
    public ResponseEntity<List<Tramo>> listarTramos() {
        List<Tramo> tramos = tramoRepository.findAll();
        return ResponseEntity.ok(tramos);
    }

    @GetMapping("/depositos")
    @Operation(summary = "Lista todos los depósitos", description = "Rol: OPERADOR")
    public ResponseEntity<List<Deposito>> listarDepositos() {
        return ResponseEntity.ok(depositoRepository.findAll());
    }

    @PostMapping("/depositos")
    @Operation(summary = "Registra un nuevo depósito", description = "Rol: OPERADOR")
    public ResponseEntity<Deposito> crearDeposito(@RequestBody Deposito deposito) {
        Deposito nuevoDeposito = depositoRepository.save(deposito);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoDeposito);
    }

    @GetMapping("/rutas")
    @Operation(summary = "Lista todas las rutas", description = "Rol: OPERADOR")
    public ResponseEntity<List<Ruta>> listarRutas() {
        List<Ruta> rutas = rutaRepository.findAll();
        return ResponseEntity.ok(rutas);
    }

    @GetMapping("/rutas/{idRuta}")
    @Operation(summary = "Obtiene una ruta por ID", description = "Rol: OPERADOR")
    public ResponseEntity<Ruta> obtenerRutaPorId(@PathVariable Long idRuta) {
        return rutaRepository.findById(idRuta)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/rutas/solicitud/{idSolicitud}")
    @Operation(summary = "Obtiene la ruta asociada a una solicitud", description = "Rol: OPERADOR")
    public ResponseEntity<Ruta> obtenerRutaPorIdSolicitud(@PathVariable Long idSolicitud) {
        return rutaRepository.findByIdSolicitud(idSolicitud)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tramos/{idTramo}")
    @Operation(summary = "Obtiene un tramo por ID", description = "Rol: OPERADOR / TRANSPORTISTA")
    public ResponseEntity<Tramo> obtenerTramoPorId(@PathVariable Long idTramo) {
        return tramoRepository.findById(idTramo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tramos/por-camion")
    @Operation(
        summary = "Lista los tramos de un camión",
        description = "Filtra por patente y opcionalmente por estado"
    )
    public ResponseEntity<List<Tramo>> tramosPorCamion(
            @RequestParam String patente,
            @RequestParam(required = false) String estado
    ) {
        List<Tramo> tramos;

        if (estado != null && !estado.isBlank()) {
            EstadoTramo estadoTramo;
            try {
                estadoTramo = EstadoTramo.valueOf(estado.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
            tramos = tramoRepository.findByPatenteCamionAndEstado(patente, estadoTramo);
        } else {
            tramos = tramoRepository.findByPatenteCamion(patente);
        }

        return ResponseEntity.ok(tramos);
    }

    @GetMapping("/tramos/pendientes")
    @Operation(
        summary = "Lista los tramos pendientes de asignar camión",
        description = "Tramos en estado ESTIMADO y sin camión asignado"
    )
    public ResponseEntity<List<Tramo>> tramosPendientes() {
        List<Tramo> tramos = tramoRepository
                .findByEstadoAndPatenteCamionIsNull(EstadoTramo.ESTIMADO);

        return ResponseEntity.ok(tramos);
    }

    @GetMapping("/depositos/{idDeposito}/tramos-finalizados")
    @Operation(
        summary = "Lista tramos que finalizaron en un depósito",
        description = "Sirve como base para ver qué contenedores están en un depósito"
    )
    public ResponseEntity<List<Tramo>> tramosFinalizadosEnDeposito(@PathVariable Long idDeposito) {
        List<Tramo> tramos = tramoRepository.findAll().stream()
                .filter(t -> t.getDepositoDestino() != null
                        && t.getDepositoDestino().getIdDeposito().equals(idDeposito)
                        && t.getEstado() == EstadoTramo.FINALIZADO)
                .toList();

        return ResponseEntity.ok(tramos);
    }

    @PostMapping("/solicitudes/{idSolicitud}/rutas/alternativas")
    @Operation(
        summary = "Genera rutas alternativas para una solicitud",
        description = "Calcula costos y tiempos de varias rutas (directa y con distintos depósitos) sin persistir en BD"
    )
    public ResponseEntity<List<RutaAlternativaDTO>> generarRutasAlternativasPorSolicitud(
            @PathVariable Long idSolicitud,
            @RequestBody RutasAlternativasRequestDTO request
    ) {
        request.setIdSolicitud(idSolicitud);
        List<RutaAlternativaDTO> alternativas = logisticaService.generarRutasAlternativas(request);
        return ResponseEntity.ok(alternativas);
    }
}
package com.gestiontransporte.logistica.controller;

import com.gestiontransporte.logistica.dto.AsignarCamionRequestDTO;
import com.gestiontransporte.logistica.dto.CambiarEstadoTramoRequestDTO;
import com.gestiontransporte.logistica.dto.CrearRutaRequestDTO;
import com.gestiontransporte.logistica.models.Deposito;
import com.gestiontransporte.logistica.models.Ruta;
import com.gestiontransporte.logistica.models.Tramo;
import com.gestiontransporte.logistica.repositories.DepositoRepository; // Rápido para CRUD
import com.gestiontransporte.logistica.repositories.RutaRepository;
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
    private final DepositoRepository depositoRepository; // Para el CRUD simple de Depósitos
    private final RutaRepository rutaRepository;

    public LogisticaController(LogisticaService logisticaService, DepositoRepository depositoRepository, RutaRepository rutaRepository) {
        this.logisticaService = logisticaService;
        this.depositoRepository = depositoRepository;
        this.rutaRepository = rutaRepository;
    }

    // --- Endpoints de Rutas y Tramos (Lógica Compleja) ---

    // @PostMapping("/rutas/calcular")
    // @Operation(summary = "Calcula y crea una ruta para una solicitud", description = "Rol: OPERADOR")
    // @PreAuthorize("hasRole('OPERADOR')")
    // public ResponseEntity<Ruta> calcularRuta(@RequestParam Long idSolicitud) {
    //     Ruta nuevaRuta = logisticaService.crearRutaParaSolicitud(idSolicitud);
    //     return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRuta);
    // }

    @PostMapping("/rutas/calcular")
    @Operation(summary = "Calcula y crea una ruta para una solicitud", description = "Rol: OPERADOR")
    // @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Ruta> calcularRuta(@RequestBody CrearRutaRequestDTO request) {
        System.out.println(">>> LLEGÓ A /rutas/calcular: " + request);
        Ruta nuevaRuta = logisticaService.crearRutaParaSolicitud(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRuta);
}

    @PostMapping("/rutas/alternativas")
    @Operation(
        summary = "Genera rutas alternativas para una solicitud",
        description = "Calcula costos y tiempos de varias rutas (directa y con distintos depósitos) sin persistir en BD"
    )
    // @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<List<RutaAlternativaDTO>> generarRutasAlternativas(
            @RequestBody RutasAlternativasRequestDTO request
    ) {
        List<RutaAlternativaDTO> alternativas = logisticaService.generarRutasAlternativas(request);
        return ResponseEntity.ok(alternativas);
    }


    @PostMapping("/tramos/{idTramo}/asignar-camion")
    @Operation(summary = "Asigna un camión a un tramo", description = "Rol: OPERADOR")
    //@PreAuthorize("hasRole('OPERADOR')")
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
            request.getCostoReal()  // ahora también pasa el costo real
    );
    return ResponseEntity.ok(tramoActualizado);
}

    // --- Endpoints de Depósitos (CRUD Simple - Req. Func. 10) ---

    @GetMapping("/depositos")
    @Operation(summary = "Lista todos los depósitos", description = "Rol: OPERADOR")
    //@PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<List<Deposito>> listarDepositos() {
        return ResponseEntity.ok(depositoRepository.findAll());
    }

    @PostMapping("/depositos")
    @Operation(summary = "Registra un nuevo depósito", description = "Rol: OPERADOR")
    //@PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<Deposito> crearDeposito(@RequestBody Deposito deposito) {
        Deposito nuevoDeposito = depositoRepository.save(deposito);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoDeposito);
    }

    // ================== GET RUTAS ==================

    @GetMapping("/rutas")
    @Operation(summary = "Lista todas las rutas", description = "Rol: OPERADOR")
    // @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<List<Ruta>> listarRutas() {
        return ResponseEntity.ok(rutaRepository.findAll());
    }

    @GetMapping("/rutas/{idRuta}")
    @Operation(summary = "Obtiene una ruta por ID", description = "Rol: OPERADOR")
    // @PreAuthorize("hasRole('OPERADOR')")
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


    
}
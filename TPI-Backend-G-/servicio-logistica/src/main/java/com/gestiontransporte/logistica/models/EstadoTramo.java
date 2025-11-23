package com.gestiontransporte.logistica.models;

public enum EstadoTramo {
    ESTIMADO,       // recién calculado
    ASIGNADO,       // tiene camión
    EN_TRASLADO,    // en curso
    FINALIZADO      // completado
}

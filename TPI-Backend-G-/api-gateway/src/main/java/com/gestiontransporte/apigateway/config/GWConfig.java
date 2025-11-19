package com.gestiontransporte.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GWConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

            // ============================================================
            //      MICROSERVICIO SOLICITUD (context-path: /api/solicitud)
            // ============================================================
            .route("servicio-solicitud", r -> r
                .path("/solicitudes/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://servicio-solicitud:8082/solicitudes"))


            // ============================================================
            //      MICROSERVICIO TRANSPORTE (sin context-path)
            // ============================================================

            // ----- Camiones -----
            .route("servicio-transporte-camiones", r -> r
                .path("/camiones/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://servicio-transporte:8083/transporte/camiones"))

            // ----- Transportistas -----
            .route("servicio-transporte-transportistas", r -> r
                .path("/transportistas/**")
                .filters(f -> f.stripPrefix(1))
                .uri("http://servicio-transporte:8083/transporte/transportistas"))

            .build();
    }
}





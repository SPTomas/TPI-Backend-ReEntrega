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
                .path("/solicitudes/**", "/contenedores/**")
                .uri("http://servicio-solicitud:8082"))


            // ============================================================
            //      MICROSERVICIO TRANSPORTE 
            // ============================================================


            .route("servicio-transporte", r -> r
                .path("/transporte/**")
                .uri("http://servicio-transporte:8083"))

 
            // ============================================================
            //      MICROSERVICIO logistica (sin context-path)
            // ============================================================

            .route("servicio-logistica", r -> r
                .path("/api/logistica/**")
                .uri("http://servicio-logistica:8084"))

            .build();
    }
}





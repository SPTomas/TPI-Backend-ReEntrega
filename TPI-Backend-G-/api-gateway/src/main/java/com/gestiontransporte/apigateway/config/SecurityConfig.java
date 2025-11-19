package com.gestiontransporte.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            // Deshabilitar CSRF (necesario para que Postman funcione sin tokens raros)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Configurar las reglas de autorización
            .authorizeExchange(exchanges -> exchanges
                // ¡¡PERMITIR TODO!! (Solo para desarrollo/pruebas)
                .anyExchange().permitAll()
            );
            
        return http.build();
    }
}
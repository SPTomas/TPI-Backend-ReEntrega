package com.gestiontransporte.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .pathMatchers("/transporte/transportistas/**").hasRole("admin")
                .pathMatchers("/transporte/camiones/**").hasRole("admin")

                .pathMatchers(HttpMethod.POST, "/solicitudes/**").hasRole("cliente")

                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter()))
            );
            
        return http.build();
    }
}
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
            
            // Configurar las reglas de autorizaciÃ³n
            .authorizeExchange(exchanges -> exchanges

                // ATENTO ESTO LO ESCRIBIO UN SER HUMANO PENSANTE
                // AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAH
                // grito para que prestes atencion
                // el hasRole SIEMPRE en mayusculas mismo nombre que el keycloak pero en mayusculas
                 
                //admin
                .pathMatchers("/transporte/transportistas/**").hasRole("ADMIN")
                .pathMatchers("/transporte/camiones/**").hasRole("ADMIN")
                .pathMatchers("/logistica/**").hasRole("ADMIN")
                .pathMatchers("/solicitudes/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.GET, "/precios/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.PUT, "/precios/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.GET, "/contenedores/**").hasRole("ADMIN")
                
                //transportista
                .pathMatchers(HttpMethod.POST, "/api/logistica/tramos/**").hasAnyRole("TRANSPORTISTA", "ADMIN")

                //cliente
                .pathMatchers(HttpMethod.POST, "/solicitudes/**").hasRole("CLIENTE")
                .pathMatchers(HttpMethod.GET, "/contenedores/**").hasRole("CLIENTE")

                .pathMatchers("/api/logistica/**").hasRole("ADMIN")
                

                .anyExchange().authenticated() 
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthenticationConverter()))
            );
            
        return http.build();
    }
}
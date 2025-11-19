package com.gestiontransporte.logistica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // 1. Permite el acceso a Swagger UI sin autenticación
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // 2. ¡Permite el acceso a TODOS TUS ENDPOINTS por ahora!
                .requestMatchers("/api/logistica/**").permitAll() 
                // 3. (Opcional) Permite todo el resto
                .anyRequest().permitAll() 
            )
            .csrf(csrf -> csrf.disable()); // Deshabilitamos CSRF para Postman

        return http.build();
    }
}
package com.gestiontransporte.logistica.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth

            // 🚨 ENDPOINTS INTERNOS ENTRE MICROSERVICIOS (sin seguridad)
            .requestMatchers(
                    "/api/logistica/rutas/calcular",
                    "/api/logistica/rutas/alternativas",
                    "/api/logistica/tramos/**"
            ).permitAll()

            // 🚨 ENDPOINTS ACCEDIDOS POR EL USUARIO (protegidos)
            .requestMatchers(HttpMethod.GET, "/api/logistica/depositos/**")
                    .hasAnyRole("operador", "admin")

            .requestMatchers(HttpMethod.POST, "/api/logistica/depositos/**")
                    .hasAnyRole("operador", "admin")

            .anyRequest().authenticated()
        );

        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) return Collections.emptyList();

            Object rolesObj = realmAccess.get("roles");
            if (!(rolesObj instanceof Collection<?> roles)) return Collections.emptyList();

            return roles.stream()
                    .map(Object::toString)
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toList());
        });
        return converter;
    }
}

package com.umg.sgq.configuracion;

import com.umg.sgq.seguridad.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.*;

@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CorsConfigurationSource cors(@Value("${app.cors.origins:http://localhost:4200}") String origins) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.asList(origins.split(",")));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Content-Disposition"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, FiltroAutenticacionJwt jwt, ManejadorAccesoDenegadoRest denied, PuntoEntradaAutenticacionRest entry) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(c -> {
                }).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/api/public/**", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password", "/error").permitAll().anyRequest().authenticated()).exceptionHandling(e -> e.accessDeniedHandler(denied).authenticationEntryPoint(entry))
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }
}

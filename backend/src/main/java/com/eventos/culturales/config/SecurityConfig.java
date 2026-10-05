// SPDX-License-Identifier: MIT

package com.eventos.culturales.config;

import com.eventos.culturales.services.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtService jwtService;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtService jwtService,
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins
    ) {
        this.jwtService = jwtService;
        this.allowedOrigins = List.of(allowedOrigins);
    }

    // Cadena 1: endpoints públicos explícitos (sin JWT). Todo lo que no case aquí cae a la cadena 2.
    // OJO: los GET de /eventos van con matcher a medida — /eventos/mios y /eventos/pendientes
    // NO son públicos y deben caer a la cadena 2 (JWT + @PreAuthorize). Un simple "/eventos/**"
    // o "/eventos/{id}" también casaría con ellos (un segmento cualquiera), por eso la exclusión explícita.
    @Bean
    @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http,
                                           LoginRateLimitFilter loginRateLimitFilter,
                                           RegistrationRateLimitFilter registrationRateLimitFilter) throws Exception {
        http
                .securityMatchers(matchers -> matchers
                        .requestMatchers("/auth/login", "/auth/refresh", "/auth/logout", "/auth/register")
                        .requestMatchers(request -> {
                            if (!HttpMethod.GET.matches(request.getMethod())) return false;
                            // Igual que AntPathRequestMatcher: servletPath + pathInfo
                            // (en MockMvc la ruta viaja en pathInfo; en Tomcat, en servletPath)
                            String pathInfo = request.getPathInfo();
                            String path = request.getServletPath()
                                    + (pathInfo != null ? pathInfo : "");
                            if ("/eventos".equals(path)) return true;
                            // Punto 2: el catálogo de etiquetas es público (los filtros lo necesitan sin login)
                            if ("/etiquetas".equals(path)) return true;
                            // Autenticados (caen a la cadena 2): mios, pendientes y favoritos
                            return path.startsWith("/eventos/")
                                    && !"/eventos/mios".equals(path)
                                    && !"/eventos/pendientes".equals(path)
                                    && !"/eventos/favoritos".equals(path);
                        })
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                )
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Rate limiting solo para POST /auth/login (el propio filtro ignora el resto),
                // antes de validar credenciales para no gastar BCrypt en peticiones bloqueadas.
                .addFilterBefore(loginRateLimitFilter, AuthorizationFilter.class)
                // 017 FR-004: 5 altas / 60s por IP en POST /auth/register (idem, solo ese path).
                .addFilterBefore(registrationRateLimitFilter, AuthorizationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    // Cadena 2: resto de endpoints, autenticación JWT obligatoria.
    @Bean
    @Order(2)
    public SecurityFilterChain authenticatedChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                // 019 FR-002: la cuenta demo no borra nada. Se crea AQUÍ y no como @Bean:
                // un bean de tipo Filter lo registra también Spring Boot como filtro del
                // servlet, que corre sin SecurityContext y se "come" este (OncePerRequestFilter).
                .addFilterBefore(new DemoAccountProtectionFilter(), AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(jwtService.getSecretKey())
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");  // Sin prefijo porque ya viene con ROLE_

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        jwtConverter.setPrincipalClaimName("sub");  // Extraer el email del claim "sub"
        return jwtConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Rate limiting en memoria para el login (ver LoginRateLimitFilter).
    @Bean
    public LoginRateLimitFilter loginRateLimitFilter(
            @Value("${app.security.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login-rate-limit.window-seconds:60}") long windowSeconds) {
        return new LoginRateLimitFilter(maxAttempts, windowSeconds, java.time.Clock.systemUTC());
    }

    // 017 FR-004: rate limit del registro público (ver RegistrationRateLimitFilter).
    @Bean
    public RegistrationRateLimitFilter registrationRateLimitFilter(
            @Value("${app.security.registration-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.security.registration-rate-limit.window-seconds:60}") long windowSeconds) {
        return new RegistrationRateLimitFilter(maxAttempts, windowSeconds, java.time.Clock.systemUTC());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOriginPatterns(allowedOrigins); // prod: fijar origen vía CORS_ALLOWED_ORIGINS
        configuration.addAllowedHeader("Authorization");
        configuration.addAllowedHeader("Content-Type");
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

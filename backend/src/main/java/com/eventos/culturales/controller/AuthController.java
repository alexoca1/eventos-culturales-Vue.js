package com.eventos.culturales.controller;

import com.eventos.culturales.dto.ActualizarUsuarioRequest;
import com.eventos.culturales.dto.LoginRequest;
import com.eventos.culturales.dto.RegisterRequest;
import com.eventos.culturales.entities.RefreshToken;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.UsuarioRepository;
import com.eventos.culturales.services.JwtService;
import com.eventos.culturales.services.RefreshTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    private static final java.util.Set<String> ROLES_VALIDOS =
            java.util.Set.of("ROLE_USER", "ROLE_ORGANIZADOR", "ROLE_ADMIN");

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}") String cookieSameSite
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
            );

            Usuario usuario = (Usuario) authentication.getPrincipal();
            String accessToken = jwtService.generateToken(usuario);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(usuario);

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .sameSite(cookieSameSite)
                    .maxAge(Duration.ofDays(7))
                    .build();

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("token", accessToken);
            responseBody.put("accessToken", accessToken);
            responseBody.put("user", Map.of(
                    "id", usuario.getId(),
                    "email", usuario.getEmail(),
                    "nombre", usuario.getNombre(),
                    "apellidos", usuario.getApellidos(),
                    "roles", usuario.getRoles()
            ));

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(responseBody);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en el servidor"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshTokenCookie) {
        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Cookie de refresh token no encontrada"));
        }

        try {
            RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshTokenCookie);
            Usuario usuario = newRefreshToken.getUsuario();
            String newAccessToken = jwtService.generateToken(usuario);

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken.getToken())
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .sameSite(cookieSameSite)
                    .maxAge(Duration.ofDays(7))
                    .build();

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("token", newAccessToken);
            responseBody.put("accessToken", newAccessToken);
            responseBody.put("user", Map.of(
                    "id", usuario.getId(),
                    "email", usuario.getEmail(),
                    "nombre", usuario.getNombre(),
                    "apellidos", usuario.getApellidos(),
                    "roles", usuario.getRoles(),
                    "enabled", usuario.getEnabled()
            ));

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(responseBody);

        } catch (BadCredentialsException e) {
            // Si el token es inválido o reutilizado, limpiar la cookie en el cliente
            ResponseCookie cleanCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .sameSite(cookieSameSite)
                    .maxAge(0)
                    .build();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar el refresco del token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refreshToken", required = false) String refreshTokenCookie) {
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            refreshTokenService.revokeRefreshToken(refreshTokenCookie);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "Sesión cerrada correctamente"));
    }

    // Registro público: SIEMPRE crea ROLE_USER, cualquier intento de pedir otro rol se ignora.
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (usuarioRepository.findByEmail(registerRequest.email()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El email ya está registrado"));
            }

            Usuario usuario = new Usuario();
            usuario.setEmail(registerRequest.email());
            usuario.setPassword(passwordEncoder.encode(registerRequest.password()));
            usuario.setNombre(registerRequest.nombre());
            usuario.setApellidos(registerRequest.apellidos());
            usuario.setRoles(Set.of("ROLE_USER"));
            usuario.setEnabled(true);
            usuario.setTelefono(registerRequest.telefono());

            usuarioRepository.save(usuario);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Usuario registrado correctamente"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar usuario"));
        }
    }

    // Crear un administrador adicional: SOLO un admin autenticado (cierra el agujero de padel)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/usuarios-admin")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (usuarioRepository.findByEmail(registerRequest.email()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El email ya está registrado"));
            }

            Usuario usuario = new Usuario();
            usuario.setEmail(registerRequest.email());
            usuario.setPassword(passwordEncoder.encode(registerRequest.password()));
            usuario.setNombre(registerRequest.nombre());
            usuario.setApellidos(registerRequest.apellidos());
            usuario.setRoles(Set.of("ROLE_ADMIN"));
            usuario.setEnabled(true);
            usuario.setTelefono(registerRequest.telefono());

            usuarioRepository.save(usuario);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Administrador registrado correctamente"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al registrar administrador"));
        }
    }

    @GetMapping("/perfil")
    public ResponseEntity<?> getPerfil(Authentication authentication) {
        // Extraer el JWT
        org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();

        // Obtener email del JWT
        String email = jwt.getSubject();

        // Buscar usuario en la base de datos
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Map<String, Object> perfil = new HashMap<>();
        perfil.put("id", usuario.getId());
        perfil.put("email", usuario.getEmail());
        perfil.put("nombre", usuario.getNombre());
        perfil.put("apellidos", usuario.getApellidos());
        perfil.put("fechaRegistro", usuario.getFechaRegistro());
        perfil.put("roles", usuario.getRoles());
        perfil.put("enabled", usuario.getEnabled());

        return ResponseEntity.ok(perfil);
    }

    // Editar roles y estado de un usuario (solo ADMIN). La protección de
    // auto-bloqueo (no desactivarse/quitarse ROLE_ADMIN a sí mismo) va en US3.
    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id,
                                               @RequestBody ActualizarUsuarioRequest req,
                                               Authentication authentication) {
        var opt = usuarioRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Usuario usuario = opt.get();
        if (req.roles() != null) {
            if (req.roles().isEmpty() || !ROLES_VALIDOS.containsAll(req.roles())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Roles inválidos: deben ser un conjunto no vacío de ROLE_USER, ROLE_ORGANIZADOR, ROLE_ADMIN"));
            }
        }
        // US3: si el cambio te dejara a ti mismo sin ROLE_ADMIN o deshabilitado, 409 antes de guardar nada
        java.util.Set<String> rolesFinales = req.roles() != null
                ? req.roles() : usuario.getRoles();
        Boolean enabledFinal = req.enabled() != null ? req.enabled() : usuario.getEnabled();
        if (esUnoMismo(authentication, id)
                && (!rolesFinales.contains("ROLE_ADMIN") || Boolean.FALSE.equals(enabledFinal))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "No puedes desactivarte ni quitarte ROLE_ADMIN a ti mismo"));
        }
        if (req.roles() != null) {
            usuario.setRoles(new java.util.HashSet<>(req.roles()));
        }
        if (req.enabled() != null) {
            usuario.setEnabled(req.enabled());
        }
        usuarioRepository.save(usuario);

        Map<String, Object> datos = new HashMap<>();
        datos.put("id", usuario.getId());
        datos.put("email", usuario.getEmail());
        datos.put("roles", usuario.getRoles());
        datos.put("enabled", usuario.getEnabled());
        return ResponseEntity.ok(datos);
    }

    private boolean esUnoMismo(Authentication authentication, Long id) {
        if (authentication == null) return false;
        org.springframework.security.oauth2.jwt.Jwt jwt =
                (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
        return usuarioRepository.findByEmail(jwt.getSubject())
                .map(u -> u.getId().equals(id))
                .orElse(false);
    }

    // Lista todos los usuarios registrados (solo ADMIN)
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listarUsuarios() {
        List<Map<String, Object>> usuarios = usuarioRepository.findAll().stream()
                .map(u -> {
                    Map<String, Object> datos = new HashMap<>();
                    datos.put("id", u.getId());
                    datos.put("email", u.getEmail());
                    datos.put("nombre", u.getNombre());
                    datos.put("apellidos", u.getApellidos());
                    datos.put("roles", u.getRoles());
                    datos.put("telefono", u.getTelefono());
                    datos.put("enabled", u.getEnabled());
                    return datos;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(usuarios);
    }
}

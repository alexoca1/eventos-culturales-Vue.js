// SPDX-License-Identifier: MIT

package com.eventos.culturales.controller;

import com.eventos.culturales.dto.ActualizarPerfilRequest;
import com.eventos.culturales.config.DemoAccountProtectionFilter;
import com.eventos.culturales.dto.ActualizarUsuarioRequest;
import com.eventos.culturales.dto.EventoResponseDTO;
import com.eventos.culturales.dto.LoginRequest;
import com.eventos.culturales.dto.RegisterRequest;
import com.eventos.culturales.dto.UserDataExportDTO;
import com.eventos.culturales.entities.Favorito;
import com.eventos.culturales.entities.RefreshToken;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.repositories.FavoritoRepository;
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
import java.time.Instant;
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
    private final EventoRepository eventoRepository;
    private final FavoritoRepository favoritoRepository;
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
            EventoRepository eventoRepository,
            FavoritoRepository favoritoRepository,
            @Value("${app.cookie.secure:false}") boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}") String cookieSameSite
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.eventoRepository = eventoRepository;
        this.favoritoRepository = favoritoRepository;
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

        Map<String, Object> perfil = mapaPerfil(usuario);

        return ResponseEntity.ok(perfil);
    }

    private static Map<String, Object> mapaPerfil(Usuario usuario) {
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("id", usuario.getId());
        perfil.put("email", usuario.getEmail());
        perfil.put("nombre", usuario.getNombre());
        perfil.put("apellidos", usuario.getApellidos());
        perfil.put("telefono", usuario.getTelefono());
        perfil.put("fechaRegistro", usuario.getFechaRegistro());
        perfil.put("roles", usuario.getRoles());
        perfil.put("enabled", usuario.getEnabled());
        perfil.put("nombreOrganizacion", usuario.getNombreOrganizacion());
        perfil.put("encargadoNombre", usuario.getEncargadoNombre());
        perfil.put("encargadoTelefono", usuario.getEncargadoTelefono());
        perfil.put("encargadoEmail", usuario.getEncargadoEmail());
        return perfil;
    }

    // Punto 4: cada usuario edita sus propios datos (nunca rol ni estado).
    // Teléfono obligatorio (no se puede vaciar); los datos de organización solo
    // aplican si es ORGANIZADOR (para el resto se ignoran) y deben quedar completos.
    // 023: el email también es editable, con trim, no-op si no cambia, 403 en la cuenta
    // demo y 409 si el correo ya está en uso.
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest req,
                                              Authentication authentication) {
        org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 023: el email es editable, pero es el `subject` del JWT. Guardarlo sin más
        // deja al token actual sin identificar a nadie (todo request posterior buscaría
        // por un email que ya no existe), así que el cliente renueva el token después.
        // No se pide la contraseña actual: lo que protege de verdad un cambio de email es
        // verificar el correo nuevo, no reautenticar (ver spec 023, limitaciones).
        if (req.email() != null) {
            String emailNuevo = req.email().trim();
            if (!emailNuevo.isBlank() && !emailNuevo.equalsIgnoreCase(usuario.getEmail())) {
                if (DemoAccountProtectionFilter.DEMO_EMAIL.equalsIgnoreCase(usuario.getEmail())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Acción no permitida en modo demostración. Esta cuenta tiene permisos limitados para proteger los datos."));
                }
                // Usuario.email es UNIQUE: sin esta comprobación el save revienta con
                // DataIntegrityViolationException y el usuario ve un 500 sin explicación.
                if (usuarioRepository.findByEmail(emailNuevo).isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Ese correo ya está en uso"));
                }
                usuario.setEmail(emailNuevo);
            }
        }

        if (req.nombre() != null) {
            if (req.nombre().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
            }
            usuario.setNombre(req.nombre());
        }
        if (req.apellidos() != null) {
            if (req.apellidos().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los apellidos son obligatorios"));
            }
            usuario.setApellidos(req.apellidos());
        }
        if (req.telefono() != null) {
            if (req.telefono().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El teléfono es obligatorio"));
            }
            usuario.setTelefono(req.telefono());
        }
        boolean esOrg = usuario.getRoles() != null && usuario.getRoles().equals(Set.of("ROLE_ORGANIZADOR"));
        if (esOrg) {
            String nombreOrg = req.nombreOrganizacion() != null ? req.nombreOrganizacion() : usuario.getNombreOrganizacion();
            String encNombre = req.encargadoNombre() != null ? req.encargadoNombre() : usuario.getEncargadoNombre();
            String encTel = req.encargadoTelefono() != null ? req.encargadoTelefono() : usuario.getEncargadoTelefono();
            String encEmail = req.encargadoEmail() != null ? req.encargadoEmail() : usuario.getEncargadoEmail();
            if (isBlank(nombreOrg) || isBlank(encNombre) || isBlank(encTel) || isBlank(encEmail)) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Para el rol ORGANIZADOR son obligatorios el nombre de la organización y los datos del encargado (nombre, teléfono y correo)"));
            }
            if (req.nombreOrganizacion() != null) usuario.setNombreOrganizacion(req.nombreOrganizacion());
            if (req.encargadoNombre() != null) usuario.setEncargadoNombre(req.encargadoNombre());
            if (req.encargadoTelefono() != null) usuario.setEncargadoTelefono(req.encargadoTelefono());
            if (req.encargadoEmail() != null) usuario.setEncargadoEmail(req.encargadoEmail());
        }
        usuarioRepository.save(usuario);
        return ResponseEntity.ok(mapaPerfil(usuario));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // 018 FR-001: supresión de cuenta (derecho al olvido). Se anonimiza la fila en vez de
    // borrarla: los eventos que creó siguen apuntando a `creadoPor`, así que se disocian de
    // la PERSONA (no queda email, nombre, teléfono, apellidos ni datos del encargado) pero
    // se conserva la trazabilidad que exige la LSSI. El access JWT sigue siendo válido hasta
    // que expire (stateless, 15 min); los refresh tokens sí se borran, así que la sesión no
    // sobrevive al siguiente refresh y con el email original ya no se puede volver a entrar.
    // Orden: los borrados primero y el save al final — si algo fallara a medias, el usuario
    // sigue localizable por su email original y el reintento termina el trabajo.
    @DeleteMapping("/perfil")
    public ResponseEntity<?> eliminarPerfil(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);

        refreshTokenService.revokeAllFor(usuario);
        favoritoRepository.deleteByUsuario(usuario);

        usuario.setEmail("deleted_" + Instant.now().getEpochSecond() + "@removed.local");
        usuario.setNombre("Usuario eliminado");
        usuario.setPassword("");   // columna nullable=false: no puede quedar null
        usuario.setEnabled(false);
        usuario.setTelefono(null);
        usuario.setApellidos(null);
        usuario.setNombreOrganizacion(null);
        usuario.setEncargadoNombre(null);
        usuario.setEncargadoTelefono(null);
        usuario.setEncargadoEmail(null);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("message", "Cuenta eliminada y datos anonimizados"));
    }

    // 018 FR-002: portabilidad de datos (art. 20 RGPD) — extracto JSON con todo lo que
    // el sistema tiene del usuario. Nunca se incluye el hash de la contraseña.
    @GetMapping("/perfil/exportar")
    public ResponseEntity<UserDataExportDTO> exportarPerfil(Authentication authentication) {
        Usuario usuario = usuarioActual(authentication);

        List<EventoResponseDTO> creados = eventoRepository.findByCreadoPor(usuario).stream()
                .map(EventoResponseDTO::fromEntity)
                .toList();
        List<EventoResponseDTO> favoritos = favoritoRepository.findByUsuario(usuario).stream()
                .map(Favorito::getEvento)
                .map(EventoResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(new UserDataExportDTO(
                usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.getApellidos(),
                usuario.getTelefono(), usuario.getNombreOrganizacion(),
                usuario.getEncargadoNombre(), usuario.getEncargadoTelefono(), usuario.getEncargadoEmail(),
                usuario.getRoles(), usuario.getFechaRegistro(), creados, favoritos));
    }

    private Usuario usuarioActual(Authentication authentication) {
        org.springframework.security.oauth2.jwt.Jwt jwt =
                (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
        return usuarioRepository.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
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
            if (req.roles().size() != 1 || !ROLES_VALIDOS.containsAll(req.roles())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Roles inválidos: debe contener exactamente un rol de ROLE_USER, ROLE_ORGANIZADOR, ROLE_ADMIN"));
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
        // Punto 4: pasar a ORGANIZADOR exige los datos de organización (los que ya
        // tenga guardados valen: solo se rechaza si el estado final queda incompleto).
        // Va DESPUÉS del 409: la autoprotección manda sobre cualquier otro error.
        if (rolesFinales != null && rolesFinales.equals(Set.of("ROLE_ORGANIZADOR"))) {
            String nombreOrg = req.nombreOrganizacion() != null ? req.nombreOrganizacion() : usuario.getNombreOrganizacion();
            String encNombre = req.encargadoNombre() != null ? req.encargadoNombre() : usuario.getEncargadoNombre();
            String encTel = req.encargadoTelefono() != null ? req.encargadoTelefono() : usuario.getEncargadoTelefono();
            String encEmail = req.encargadoEmail() != null ? req.encargadoEmail() : usuario.getEncargadoEmail();
            if (isBlank(nombreOrg) || isBlank(encNombre) || isBlank(encTel) || isBlank(encEmail)) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Para el rol ORGANIZADOR son obligatorios el nombre de la organización y los datos del encargado (nombre, teléfono y correo)"));
            }
        }
        if (req.roles() != null) {
            usuario.setRoles(new java.util.HashSet<>(req.roles()));
        }
        if (req.enabled() != null) {
            usuario.setEnabled(req.enabled());
        }
        if (req.nombreOrganizacion() != null) usuario.setNombreOrganizacion(req.nombreOrganizacion());
        if (req.encargadoNombre() != null) usuario.setEncargadoNombre(req.encargadoNombre());
        if (req.encargadoTelefono() != null) usuario.setEncargadoTelefono(req.encargadoTelefono());
        if (req.encargadoEmail() != null) usuario.setEncargadoEmail(req.encargadoEmail());
        usuarioRepository.save(usuario);

        Map<String, Object> datos = new HashMap<>();
        datos.put("id", usuario.getId());
        datos.put("email", usuario.getEmail());
        datos.put("roles", usuario.getRoles());
        datos.put("enabled", usuario.getEnabled());
        datos.put("nombreOrganizacion", usuario.getNombreOrganizacion());
        datos.put("encargadoNombre", usuario.getEncargadoNombre());
        datos.put("encargadoTelefono", usuario.getEncargadoTelefono());
        datos.put("encargadoEmail", usuario.getEncargadoEmail());
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
                    datos.put("nombreOrganizacion", u.getNombreOrganizacion());
                    datos.put("encargadoNombre", u.getEncargadoNombre());
                    datos.put("encargadoTelefono", u.getEncargadoTelefono());
                    datos.put("encargadoEmail", u.getEncargadoEmail());
                    return datos;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(usuarios);
    }
}

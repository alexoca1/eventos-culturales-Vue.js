// SPDX-License-Identifier: MIT

package com.eventos.culturales.config;

import com.eventos.culturales.entities.Etiqueta;
import com.eventos.culturales.entities.Evento;
import com.eventos.culturales.entities.Usuario;
import com.eventos.culturales.repositories.EtiquetaRepository;
import com.eventos.culturales.repositories.EventoRepository;
import com.eventos.culturales.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer {

    // 019 T176: iframe genérico que comparten las 3 semillas renombradas (ver seedEventos).
    private static final String MAPA_EMBED_SEMILLA =
            "<iframe src=\"https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3114.1514033241547!2d-4.109704738305717!3d38.69136633423611!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd6b8cf6636e3549%3A0x4b13ec5f916e923c!2sAuditorio%20Municipal!5e0!3m2!1ses!2ses!4v1733934475151!5m2!1ses!2ses\" width=\"400\" height=\"300\" style=\"border:0;\" allowfullscreen=\"\" loading=\"lazy\" referrerpolicy=\"no-referrer-when-downgrade\"></iframe>";

    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminSeedPassword;

    public DataInitializer(
            UsuarioRepository usuarioRepository,
            EventoRepository eventoRepository,
            EtiquetaRepository etiquetaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_SEED_PASSWORD:admin123}") String adminSeedPassword
    ) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.etiquetaRepository = etiquetaRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminSeedPassword = adminSeedPassword;
    }

    @PostConstruct
    public void init() {
        String adminEmail = "admin@test.com";
        var adminExistente = usuarioRepository.findByEmail(adminEmail);
        if (adminExistente.isEmpty()) {
            Usuario admin = new Usuario();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminSeedPassword));
            admin.setNombre("Admin");
            admin.setApellidos("Sistema");
            admin.setRoles(Set.of("ROLE_ADMIN"));
            admin.setEnabled(true);
            admin.setTelefono("600123123");
            usuarioRepository.save(admin);
            System.out.println("[DataInitializer] Usuario admin semilla creado.");
        } else {
            Usuario admin = adminExistente.get();
            boolean cambiado = false;
            if (adminExistente.get().getTelefono() == null || adminExistente.get().getTelefono().isBlank()) {
                admin.setTelefono("600123123");
                cambiado = true;
            }
            // Migración US3: el admin anterior guardaba roles en columna String;
            // ahora van en la tabla usuario_roles (@ElementCollection)
            if (admin.getRoles() == null || admin.getRoles().isEmpty()) {
                admin.setRoles(Set.of("ROLE_ADMIN"));
                cambiado = true;
            }
            if (cambiado) {
                usuarioRepository.save(admin);
            }
        }
        // 019 FR-001: cuenta demo. La contraseña no es un secreto: es una credencial
        // pública que se publica en el README para que la gente pruebe la demo.
        String demoEmail = "demo@eventos-culturales.es";
        if (usuarioRepository.findByEmail(demoEmail).isEmpty()) {
            Usuario demo = new Usuario();
            demo.setEmail(demoEmail);
            demo.setPassword(passwordEncoder.encode("Demo1234!"));
            demo.setNombre("Demo");
            demo.setApellidos("Demostración");
            demo.setRoles(Set.of("ROLE_ADMIN"));
            demo.setEnabled(true);
            demo.setTelefono("600000000");
            usuarioRepository.save(demo);
            System.out.println("[DataInitializer] Usuario demo semilla creado.");
        }

        backfillNombres();
        seedEventos();
    }

    // 015 T150: el nombre del evento no tenía columna; lo que hoy llamamos
    // `descripcion` era el nombre. Se copia tal cual, sin truncar ni reformatear,
    // para no perder datos. Idempotente: solo toca lo que está sin nombre.
    private void backfillNombres() {
        for (Evento e : eventoRepository.findSinNombre()) {
            e.setNombre(e.getDescripcion());
            eventoRepository.save(e);
            System.out.println("[DataInitializer] Nombre migrado desde descripcion: evento " + e.getId());
        }
    }

    // Los 5 eventos de ejemplo del frontend; solo si la tabla está vacía.
    // El cartel se guarda en BD (imagen del classpath) para que todo funcione igual en producción.
    // 007: nacen APROBADO y con creadoPor = admin semilla.
    // Punto 2: primero el catálogo de etiquetas (las 7 categorías de siempre), luego los eventos.
    private void seedEventos() {
        for (String nombre : List.of("MUSICA", "TEATRO", "EXPOSICION", "CINE", "LITERATURA", "INFANTIL", "OTROS")) {
            if (etiquetaRepository.findByNombre(nombre).isEmpty()) {
                etiquetaRepository.save(new Etiqueta(null, nombre));
            }
        }
        Usuario admin = usuarioRepository.findByEmail("admin@test.com").orElse(null);
        if (eventoRepository.count() == 0) {
            List<Evento> semillas = List.of(
                    // 019 T176: los tres locales de hostelería reales sustituidos por espacios
                    // públicos (la demo no debe anunciar negocios particulares) y su `pb` de
                    // Google Maps por el del Auditorio Municipal: es el único embed genérico
                    // que ya sabemos que renderiza, y ningún `?q=` alternativo se pudo validar.
                    evento("Plaza de la Constitución", "Pl. de la Constitución, s/n, 13500 Puertollano, Ciudad Real",
                            "2026-09-01", "Fiesta Mexicana", "imagen1.jpg", List.of("INFANTIL"),
                            MAPA_EMBED_SEMILLA),
                    evento("Biblioteca Pública", "C. Numancia, 55, 13500 Puertollano, Ciudad Real",
                            "2026-10-01", "Monologo Danni Robira", "imagen2.jpg", List.of("TEATRO"),
                            MAPA_EMBED_SEMILLA),
                    evento("Mercado de Abastos", "Paseo de San Gregorio, 2D, 13500 Puertollano, Ciudad Real",
                            "2026-11-01", "Exhibición de tapas vegetarianas", "imagen3.jpg", List.of("OTROS"),
                            MAPA_EMBED_SEMILLA),
                    evento("Auditorio Municipal", "Pl. Mariana Pineda, 0, 13500 Puertollano, Ciudad Real",
                            "2026-12-01", "Rock la Mancha Festival", "imagen4.jpg", List.of("MUSICA"),
                            "<iframe src=\"https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3114.1514033241547!2d-4.109704738305717!3d38.69136633423611!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd6b8cf6636e3549%3A0x4b13ec5f916e923c!2sAuditorio%20Municipal!5e0!3m2!1ses!2ses!4v1733934475151!5m2!1ses!2ses\" width=\"400\" height=\"300\" style=\"border:0;\" allowfullscreen=\"\" loading=\"lazy\" referrerpolicy=\"no-referrer-when-downgrade\"></iframe>"),
                    evento("Museo Cristina García Rodero", "Pl. Constitución, s/n, 13500 Puertollano, Ciudad Real",
                            "2027-01-01", "Exposición de Arte", "imagen5.jpg", List.of("EXPOSICION"),
                            "<iframe src=\"https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d389.2939734827675!2d-4.1110539430529505!3d38.68676282940689!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0xd6b8c5ff5e8e3b3%3A0x5dbac4a9911589ec!2sMuseo%20Cristina%20Garc%C3%ADa%20Rodero!5e0!3m2!1ses!2ses!4v1733934734205!5m2!1ses!2ses\" width=\"400\" height=\"300\" style=\"border:0;\" allowfullscreen=\"\" loading=\"lazy\" referrerpolicy=\"no-referrer-when-downgrade\"></iframe>")
            );
            for (Evento e : semillas) {
                e.setEstado(com.eventos.culturales.entities.EstadoEvento.APROBADO);
                e.setCreadoPor(admin);
            }
            eventoRepository.saveAll(semillas);
            System.out.println("[DataInitializer] 5 eventos de ejemplo creados");
        }
        backfillCarteles();
    }

    // Migración: eventos con cartel pero sin HD, o con cartelUrl "../img/..." antigua.
    // HD = imagen completa, display = reescalado a 400px. Idempotente.
    private void backfillCarteles() {
        Usuario admin = usuarioRepository.findByEmail("admin@test.com").orElse(null);
        for (Evento e : eventoRepository.findAll()) {
            boolean cambiado = false;
            // 007: filas anteriores al flujo de aprobación = APROBADO del admin (siguen públicas)
            if (e.getEstado() == null) {
                e.setEstado(com.eventos.culturales.entities.EstadoEvento.APROBADO);
                cambiado = true;
            }
            if (e.getCreadoPor() == null && admin != null) {
                e.setCreadoPor(admin);
                cambiado = true;
            }
            if (e.getFechaFin() == null) {
                // 006: filas anteriores a fechaFin real = un día
                e.setFechaFin(e.getFecha());
                cambiado = true;
            }
            if (e.getEtiquetas() == null || e.getEtiquetas().isEmpty()) {
                // Punto 2: la antigua categoria única pasa a etiquetas (una sola).
                // Se lee en nativo porque la entidad ya no mapea esa columna.
                String legacy = null;
                try {
                    legacy = eventoRepository.categoriaLegacyDe(e.getId());
                } catch (Exception ex) {
                    legacy = null; // columna ya inexistente en BDs nuevas
                }
                String nombre = (legacy == null || legacy.isBlank()) ? "OTROS" : legacy;
                Etiqueta t = etiquetaRepository.findByNombre(nombre)
                        .orElseGet(() -> etiquetaRepository.save(new Etiqueta(null, nombre)));
                e.setEtiquetas(new java.util.HashSet<>(Set.of(t)));
                cambiado = true;
            }
            if (e.getCartel() == null && e.getCartelUrl() != null) {
                String nombre = e.getCartelUrl().substring(e.getCartelUrl().lastIndexOf('/') + 1);
                byte[] bytes = leerImagen(nombre);
                if (bytes == null) continue;
                e.setCartelHd(bytes);
                e.setCartelHdContentType("image/jpeg");
                e.setCartel(redimensionar(bytes, 400));
                e.setCartelContentType("image/jpeg");
                e.setCartelUrl(null);
                cambiado = true;
            } else if (e.getCartelHd() == null && e.getCartel() != null) {
                e.setCartelHd(e.getCartel());
                e.setCartelHdContentType(e.getCartelContentType());
                e.setCartel(redimensionar(e.getCartel(), 400));
                cambiado = true;
            }
            if (cambiado) {
                eventoRepository.save(e);
                System.out.println("[DataInitializer] Cartel migrado a BD: evento " + e.getId());
            }
        }
    }

    private static byte[] leerImagen(String nombre) {
        try (var in = DataInitializer.class.getResourceAsStream("/imagenes/" + nombre)) {
            return in == null ? null : in.readAllBytes();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private Evento evento(String establecimiento, String direccion, String fecha,
                          String descripcion, String imagen, List<String> etiquetas, String mapaEmbed) {
        Evento e = new Evento();
        e.setEstablecimiento(establecimiento);
        e.setDireccion(direccion);
        e.setFecha(LocalDate.parse(fecha));
        // 015 US1: el nombre del evento. Las semillas replican exactamente lo que
        // hadría el backfill (nombre = descripcion), así que son indistinguibles
        // de una fila migrada y el frontend sigue mostrando el texto de siempre.
        e.setNombre(descripcion);
        e.setDescripcion(descripcion);
        // El catálogo ya está sembrado arriba: los nombres existen sí o sí
        e.setEtiquetas(etiquetas.stream()
                .map(n -> etiquetaRepository.findByNombre(n).orElseThrow())
                .collect(java.util.stream.Collectors.toSet()));
        byte[] original = leerImagen(imagen);
        e.setCartelHd(original);
        e.setCartelHdContentType("image/jpeg");
        e.setCartel(redimensionar(original, 400));
        e.setCartelContentType("image/jpeg");
        e.setMapaEmbed(mapaEmbed);
        return e;
    }

    // Reescala JPEG a un ancho máximo (para la versión display del seed/backfill).
    // Solo stdlib (ImageIO); si algo falla devuelve el original.
    private static byte[] redimensionar(byte[] original, int maxAncho) {
        if (original == null) return null;
        try {
            var img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(original));
            if (img == null || img.getWidth() <= maxAncho) return original;
            int alto = img.getHeight() * maxAncho / img.getWidth();
            var mini = new java.awt.image.BufferedImage(maxAncho, alto, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = mini.createGraphics();
            g.drawImage(img, 0, 0, maxAncho, alto, null);
            g.dispose();
            var out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(mini, "jpg", out);
            return out.toByteArray();
        } catch (Exception ex) {
            return original;
        }
    }
}

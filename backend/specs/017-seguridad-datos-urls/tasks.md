# Tasks: Seguridad de datos, URLs y sanitización

---

## Phase 1: DTOs y Minimización de Datos (P1)

- [x] T170 Crear `CreadoPorResponseDTO` (`Long id, String email, String nombreOrganizacion`) y `EventoResponseDTO` mapeando la entidad `Evento`.
- [x] T171 Modificar `EventoController` para que los endpoints `GET /eventos`, `GET /eventos/{id}`, `GET /eventos/mios`, etc., devuelvan `EventoResponseDTO` en lugar de la entidad `Evento` directamente.

## Phase 2: Validación de URLs y Password (P1)

- [x] T172 Añadir regex de validación `@Pattern(regexp = "^(https?://.*)?$", message = "La URL debe comenzar por http:// o https://")` en `urlEvento`, `cartelUrl` de `EventoDTO` y `url` de `RedSocialDTO`.
- [x] T173 Actualizar `RegisterRequest` con validación de contraseña de 8+ caracteres con mayúscula, minúscula y número.

## Phase 3: Rate Limiting y Magic Bytes (P2)

- [x] T174 Crear `RegistrationRateLimitFilter` para limitar las peticiones a `POST /auth/register` (5 req / 60s por IP) y registrarlo en `SecurityConfig`.
- [x] T175 Actualizar `EventoController.validarArchivo()` para verificar magic bytes de archivos JPEG, PNG y WebP.

## Phase 4: Semillas y Limpieza (P2)

- [x] T176 Modificar `DataInitializer` sustituyendo nombres de restaurantes por sitios públicos ("Teatro Municipal", "Biblioteca Pública", "Plaza de la Constitución", "Mercado Abasto"). Hecho en la spec 019 (desvío: Puertollano **no tiene "Teatro Municipal"** — el recinto real es el Auditorio, ya sembrado —, así que los 3 locales pasan a *Plaza de la Constitución*, *Biblioteca Pública* y *Mercado de Abastos* con dirección verificada). `mapaEmbed` de esos 3 = constante `MAPA_EMBED_SEMILLA` con el `pb` del Auditorio (embed genérico ya sabido que renderiza; el alternativo `?q=Puertollano` devolvió el shell pero nunca pidió tiles, así que no se pudo validar). También se neutralizó el ejemplo de `docs/api-contract.md` (`El mesoncito` / `elmeseoncito.es`).
- [x] T177 Ejecutar `./mvnw test` y verificar que todos los tests pasen. *(212 tests, verde)*

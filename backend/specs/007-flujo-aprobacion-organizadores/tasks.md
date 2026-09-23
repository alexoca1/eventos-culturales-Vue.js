# Tasks: Flujo de Aprobación de Organizadores — Eventos Culturales Puertollano

**Input**: `specs/007-flujo-aprobacion-organizadores/spec.md` + `plan.md`

---

## Phase 1: US1 — Organizador crea evento (P1)
- [x] T078 Crear enum `EstadoEvento { PENDIENTE_REVISION, APROBADO, RECHAZADO, PENDIENTE_ELIMINACION }`
- [x] T079 Añadir a `Evento`: `estado` (default `APROBADO`), `creadoPor` (`@ManyToOne Usuario`, nullable), `motivoRechazo` (nullable)
- [x] T080 [US1] Test: organizador crea evento a +72h → `PENDIENTE_REVISION`; a +10h → 400; admin crea evento → `APROBADO` directo, sin validar 48h
- [x] T081 [US1] En `EventoController.create(...)`: si el autenticado tiene `ROLE_ORGANIZADOR` (no `ROLE_ADMIN`), validar 48h y fijar `estado=PENDIENTE_REVISION`, `creadoPor=usuario autenticado`; si es `ROLE_ADMIN`, comportamiento actual (`APROBADO` directo)

**Checkpoint**: crear evento como organizador queda pendiente y respeta las 48h; como admin, sin cambios.

---

## Phase 2: US5 — Público solo ve aprobados (P1, va antes de US2 para poder testear con datos reales)
- [x] T082 [US5] Test: con eventos en los 4 estados, `GET /eventos` sin auth solo devuelve `APROBADO`; con admin autenticado, devuelve los 4; combinando con `fecha`/`categoria` existentes
- [x] T083 [US5] En `EventoController.findAll(...)`: si el llamante no es `ROLE_ADMIN`, filtrar siempre por `estado=APROBADO` antes de devolver (independientemente de los filtros `fecha`/`categoria` ya existentes)
- [x] T084 [US5] `DataInitializer`: asignar `estado=APROBADO` y `creadoPor=admin semilla` a los 5 eventos ya sembrados

**Checkpoint**: el público nunca ve eventos pendientes/rechazados; el panel de admin sigue viendo todo.

---

## Phase 3: US2 — Admin aprueba/rechaza (P1)
- [x] T085 [US2] Test: `GET /eventos/pendientes` devuelve pendientes de cualquier organizador; `/aprobar` pasa `PENDIENTE_REVISION`→`APROBADO`; `/rechazar` con motivo pasa a `RECHAZADO` guardando el motivo; un organizador que llama a estos endpoints recibe 403
- [x] T086 [US2] Crear `RechazoRequest` (record: `motivo` opcional)
- [x] T087 [US2] `EventoRepository`: `findByEstadoInOrderByIdAsc(List<EstadoEvento>)`
- [x] T088 [US2] `EventoController`: `GET /eventos/pendientes`, `POST /eventos/{id}/aprobar`, `POST /eventos/{id}/rechazar`, todos `@PreAuthorize("hasRole('ADMIN')")`

**Checkpoint**: el admin puede ver y resolver la cola de moderación.

---

## Phase 4: US3 — Organizador edita su evento (P1)
- [x] T089 [US3] Test: organizador dueño edita evento `APROBADO`→`PENDIENTE_REVISION`; edita `RECHAZADO`→`PENDIENTE_REVISION`; edita evento `PENDIENTE_ELIMINACION`→409; organizador NO dueño→403; admin edita cualquiera→`APROBADO` directo
- [x] T090 [US3] En `EventoController.update(...)`: lógica de ownership + transición de estado descrita en FR-005

**Checkpoint**: editar como organizador respeta ownership y vuelve a poner el evento en revisión.

---

## Phase 5: US4 — Organizador solicita eliminar (P1)
- [x] T091 [US4] Test: organizador dueño pide eliminar `APROBADO`→`PENDIENTE_ELIMINACION` (no se borra); admin aprueba→borrado real; admin rechaza→vuelve a `APROBADO`; admin elimina directamente cualquier evento→sin cambios (comportamiento actual)
- [x] T092 [US4] En `EventoController.delete(...)`: si `ROLE_ORGANIZADOR` dueño → cambia a `PENDIENTE_ELIMINACION` en vez de borrar; si `ROLE_ADMIN` → comportamiento actual (borrado directo)
- [x] T093 [US4] Ajustar `/aprobar` para que, si el evento estaba `PENDIENTE_ELIMINACION`, borre en vez de pasar a `APROBADO` (ya cubierto por FR-008, verificar test T085/T091 lo prueba)

**Checkpoint**: eliminar como organizador es una solicitud, no un borrado inmediato.

---

## Phase 6: US6 — Notificación por email (P2)
- [x] T094 [US6] Añadir dependencia `spring-boot-starter-mail` a `pom.xml`
- [x] T095 [US6] Configurar `spring.mail.host/port/username/password` en `application.properties` vía env vars (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` — valores de Mailtrap)
- [x] T096 [US6] Crear `EmailService` (método `enviar(destinatario, asunto, cuerpo)`, capturando y logueando cualquier excepción de envío sin propagarla)
- [x] T097 [US6] Test: aprobar/rechazar dispara `EmailService.enviar(...)` (mockeado) con los datos correctos; si el mock lanza excepción, el estado en BD igual queda actualizado (FR-013, escenario 3)
- [x] T098 [US6] Conectar `EmailService` en los endpoints `/aprobar` y `/rechazar` de `EventoController`

**Checkpoint**: aprobar/rechazar intenta enviar email sin bloquear la operación si falla.

---

## Phase 7: Polish
- [x] T099 [P] `GET /eventos/mios` (`ROLE_ORGANIZADOR`, `findByCreadoPorOrderByIdAsc`)
- [x] T100 Revisar `SecurityConfig`: confirmar que `/eventos/mios` y `/eventos/pendientes` NO están en la cadena pública (deben exigir JWT + rol correcto)
- [x] T101 Actualizar `docs/api-contract.md` con los 4 endpoints nuevos, `estado`, `creadoPor`, `motivoRechazo`
- [x] T102 `./mvnw test` completo en verde (suite `001` a `007`)

## Dependencies & Execution Order
US1 → US5 → US2 → US3 → US4 → US6, en ese orden (cada fase depende de que el estado y el filtro existan antes de poder moderarlos o notificarlos). T099-T102 al final.

## Notas
- Cada prompt ≈ 1-2 historias (esta feature es grande; ver agrupación de prompts abajo).
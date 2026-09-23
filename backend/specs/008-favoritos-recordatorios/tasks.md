# Tasks: Favoritos y Recordatorio por Email — Eventos Culturales Puertollano

**Input**: `specs/008-favoritos-recordatorios/spec.md` + `plan.md`

---

## Phase 1: US1 — Marcar/desmarcar favorito (P1)
- [x] T112 Crear entidad `Favorito` (`usuario`, `evento`, `fechaCreacion`, `recordatorioEnviado` default `false`, restricción única `(usuario_id, evento_id)`)
- [x] T113 Crear `FavoritoRepository` (`findByUsuarioAndEvento`, `deleteByEvento`, `findByEventoFechaAndEventoEstadoAndRecordatorioEnviadoFalse`)
- [x] T114 [US1] Test: `POST /eventos/{id}/favorito` sobre evento `APROBADO` crea el favorito (idempotente si se repite); sobre evento no `APROBADO` → 404; sin autenticar → 401; `DELETE` elimina el favorito si existía
- [x] T115 [US1] `POST /eventos/{id}/favorito` y `DELETE /eventos/{id}/favorito` en `EventoController`, `@PreAuthorize("isAuthenticated()")`

**Checkpoint**: marcar/desmarcar favorito funciona con las reglas correctas.

---

## Phase 2: US2 — Ver mis favoritos (P1)
- [x] T116 [US2] Test: `GET /eventos/favoritos` devuelve solo los favoritos del usuario autenticado que siguen `APROBADO`; uno que dejó de estarlo no aparece
- [x] T117 [US2] `GET /eventos/favoritos` en `EventoController`, filtrando por usuario autenticado y `estado=APROBADO`

**Checkpoint**: "mis favoritos" siempre refleja el estado real de cada evento.

---

## Phase 3: US3 — Integridad al borrar (P1)
- [x] T118 [US3] Test: borrar un evento con favoritos (por las 2 vías: `DELETE` directo de admin, y aprobación de `PENDIENTE_ELIMINACION`) no da error, y sus favoritos desaparecen
- [x] T119 [US3] En ambos puntos de borrado definitivo de `EventoController`, llamar a `favoritoRepository.deleteByEvento(evento)` antes de borrar el evento

**Checkpoint**: nunca hay un error de integridad referencial al borrar un evento con favoritos.

---

## Phase 4: US4 — Recordatorio automático (P1)
- [x] T120 [US4] Añadir `@EnableScheduling` en `EventosCulturalesApplication`
- [x] T121 [US4] Test de `RecordatorioService` (invocando el método directamente, sin esperar al cron real): un favorito con evento `fecha=mañana`, `APROBADO`, `recordatorioEnviado=false` → dispara `EmailService.enviar(...)` (mockeado) y queda `recordatorioEnviado=true`; se ejecuta de nuevo → no se reenvía; un favorito con `fecha` != mañana → no se procesa; si el mock de `EmailService` lanza excepción para un favorito, el resto de la cola de ese día se sigue procesando igualmente
- [x] T122 [US4] Crear `RecordatorioService` con el método `@Scheduled(cron = "${app.recordatorios.cron:0 0 20 * * *}")`, que busca los favoritos candidatos, itera enviando y marcando, capturando excepciones por favorito individual sin detener el bucle

**Checkpoint**: el job envía exactamente un recordatorio por favorito elegible, nunca cero ni dos.

---

## Phase 5: Polish
- [x] T123 Actualizar `docs/api-contract.md`: los 3 endpoints nuevos de favoritos
- [x] T124 `./mvnw test` completo en verde (suite `001` a `008`)

## Dependencies & Execution Order
US1 → US2 → US3 → US4, en ese orden (cada fase se apoya en la anterior). T123-T124 al final.

## Notas
- Cada prompt ≈ 1 historia completa.
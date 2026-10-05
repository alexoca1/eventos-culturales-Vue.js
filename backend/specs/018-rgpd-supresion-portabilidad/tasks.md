# Tasks: Derechos ARCO+ (Supresión y Portabilidad RGPD)

---

## Phase 1: Endpoint Supresión (P1)

- [x] T180 Añadir endpoint `DELETE /auth/perfil` en `AuthController`.
- [x] T181 Implementar lógica de anonimización: email sustituido por `deleted_[timestamp]@removed.local`, borrado de password, eliminación de refresh tokens y eliminación de favoritos.

## Phase 2: Endpoint Portabilidad (P1)

- [x] T182 Crear `UserDataExportDTO` conteniendo datos de perfil, lista de eventos creados y lista de IDs/nombres de eventos favoritos.
- [x] T183 Añadir endpoint `GET /auth/perfil/exportar` en `AuthController`.

## Phase 3: Tests (P1)

- [x] T184 Añadir tests en `AuthControllerTest` verificando supresión y exportación de datos.

---

## Notas de implementación

- **No se borra la fila `Usuario`**: los eventos guardan `creadoPor` como FK. Borrarla
  provocaría un FK roto o forzaría un borrado en cascada de eventos ajenos al derecho que
  se ejerce. Se anonimiza en su lugar (art. 17.3 RGPD: conservación obligatoria por
  obligación legal — la LSSI mantiene la trazabilidad del prestador de la información).
- **Anonimización completa**, no solo el email: teléfono, apellidos, nombre de la
  organización y los tres campos del encargado van a `null` (columnas nullable). `password`
  se vacía en vez de `null` porque la columna es `nullable=false`.
- **Roles y eventos se conservan**; `enabled=false` por si acaso.
- **Orden de operaciones**: `revokeAllFor` → `deleteByUsuario` → `save` al final. Si algo
  fallara a mitad, el usuario sigue localizable por su email original y el reintento
  termina el trabajo (tras guardar, el email original ya no existe y reintentar no encuentra
  la cuenta).
- **Refresh tokens y favoritos** se borran en métodos propios con `@Transactional`
  (`RefreshTokenService.revokeAllFor`, `FavoritoRepository.deleteByUsuario`). El controller
  no lleva `@Transactional` — convención del repo: cada transacción se abre donde está el
  borrado, igual que en `EventoController.deleteEvento`.
- El **access JWT** sigue siendo válido 15 min tras la supresión (resource server stateless,
  sin lista negra); el siguiente `refresh` falla porque ya no hay usuario.
- `GET /auth/perfil/exportar` incluye eventos creados **en cualquier estado** y todos los
  favoritos (no solo los `APROBADO`): es la copia de datos que corresponde al interesado.
- `AuthController` ganó dos dependencias de constructor (`EventoRepository`,
  `FavoritoRepository`), así que `AuthAdminTest` y `LoginBodyDebugTest` necesitan los
  `@MockitoBean` correspondientes.

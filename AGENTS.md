# AGENTS.md — eventos-culturales-Vue.js

Monorepo con dos subproyectos independientes, cada uno con su propio `AGENTS.md` y su
propio flujo de spec-driven development (speckit). Lee el que corresponda **antes** de
tocar código en esa carpeta:

- **`backend/AGENTS.md`** — API REST Spring Boot 4 / Java 21 / MySQL. Constitución en
  `backend/.specify/memory/constitution.md` (reglas NO negociables: constructor injection,
  seguridad explícita, tests obligatorios, etc.).
- **`frontend/AGENTS.md`** — App Vue 3 sin build que consume esa API.

## Contrato entre ambos

- `docs/api-contract.md` es la fuente de verdad de la API (rutas, roles, modelos, errores).
  Cualquier cambio de contrato debe actualizarse ahí y reflejarse en ambos `AGENTS.md` si
  afecta a cómo se documentan.
- El frontend nunca debe asumir estructura interna del backend (entidades, servicios) más
  allá de lo que expone `docs/api-contract.md`.

## Visión general

Web de eventos culturales de Puertollano (Ciudad Real): el invitado busca eventos por
fecha sin login; el admin hace CRUD completo autenticado con JWT. Ver `README.md` para
cómo ejecutar el proyecto completo (backend + frontend) y las credenciales de demo.
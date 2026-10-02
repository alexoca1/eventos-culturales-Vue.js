# Implementation Plan: Nombre, descripción y galería + contacto y redes — Frontend

**Branch**: `019-nombre-descripcion-galeria` | **Date**: 2026-09-25 (F181-F188) / 2026-09-30 (F189-F192)

## Summary
El formulario separa "nombre" (obligatorio) de "descripción" (opcional), gana una
galería de hasta 5 fotos subidas tras guardar el evento, y desde el 2026-09-30 también
teléfono, URL del evento y hasta 8 redes sociales (red + URL). Las tarjetas públicas
muestran miniaturas de galería con lightbox y, al final de cada `datos_eventos`,
el teléfono (enlace `tel:`), la URL ("Más información", `target=_blank rel=noopener`)
y las redes como pastillas clicables con el texto legible de `textoRed()`.
Todo con variables CSS ya definidas. Sin dependencias nuevas; verificable con
`node --check` y `node js/ui/galeria.check.js`.

## Project Structure
```text
frontend/js/api/eventos.js                # apiToView: +nombre, +description, +redes, +telefono, +urlEvento
frontend/js/composables/useEventoForm.js  # inputNombre/Descripcion + inputRedes/Telefono/UrlEvento,
                                          # añadirRed/quitarRed, precarga en editEvento/previsualizar,
                                          # DTO con redesSociales/telefonoEvento/urlEvento, sincronizarGaleria
frontend/js/ui/format.js                  # + textoRed(red): enum RedSocial -> texto legible
frontend/js/ui/galeria.check.js           # check ampliado: 6 tarjetas con miniaturas + contacto/redes,
                                          # 5 slots, campos del formulario, contrato de red de galería
frontend/js/pages/{admin,organizador,invitado}.js  # exponen textoRed en el setup()
frontend/views/administrador.html         # formulario + nombrar/descripción/galería/tel/URL/redes + 2 tarjetas
frontend/views/organizador.html           # ídem + tarjeta de previsualización (previewEv.*)
frontend/views/usuarioEstandar.html       # 3 tarjetas con contacto/redes
frontend/css/eventos.css                  # + .redes-evento, .red-evento-link, .red-social-row
frontend/specs/019-nombre-descripcion-galeria/  # spec.md, plan.md, tasks.md (F181-F192)
```

## Fixture de datos (para la prueba manual en navegador)
- Evento con nombre "Concierto de cierre", descripción larga, cartel + 3 fotos,
  teléfono `+34 600 111 222`, URL `https://ejemplo.org` y 2 redes (FACEBOOK, INSTAGRAM).
- Verificar en invitado: tarjeta con miniaturas, `tel:`, "Más información" y las 2 redes
  con su texto legible; lightbox en cartel y fotos.

## Complexity Tracking
| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Fila de red con `<select>` + enlace por tarjeta | El enum lo exige el backend y el texto legible pide mapa | Un único campo de texto libre rompería la validación del backend |
| `textoRed` con fallback | El enum puede crecer sin tocar el frontend | Romper en rojo ante un valor desconocido sería peor UX que pintarlo tal cual |
| La red con URL vacía se envía al backend | Una sola fuente de verdad para validar | Validar URL en el frontend duplicaría reglas y el mensaje del backend ya es claro |
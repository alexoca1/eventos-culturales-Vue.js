# Implementation Plan: Cuenta demo protegida y auditoría de moderación

**Branch**: `019-cuenta-demo-protegida` | **Date**: 2026-10-03

## Summary

1. Sembrar `demo@eventos-culturales.es` en `DataInitializer`.
2. Crear `DemoAccountProtectionFilter` que bloquea solicitudes `DELETE` de la cuenta demo.
3. Añadir `moderadoPor` y `fechaModeracion` a `Evento` y actualizarlos en `aprobar`/`rechazar`.

## Project Structure

```text
backend/src/main/java/com/eventos/culturales/
├── config/
│   ├── DemoAccountProtectionFilter.java (nuevo filtro)
│   ├── DataInitializer.java (semilla usuario demo)
│   └── SecurityConfig.java
├── entities/
│   └── Evento.java (nuevos campos)
├── controller/
│   └── EventoController.java (actualización moderación)
└── src/test/java/...
```

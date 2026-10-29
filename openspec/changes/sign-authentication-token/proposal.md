# Proposal

## Why

[Issue #24 (HU-01)](https://github.com/user-bcp-student03/ai-labs/issues/24) addresses H-AUTH-03: the authentication token is currently only Base64-encoded and has no cryptographic integrity, so its contents can be altered without detection. This leaves an authentication artifact untrustworthy even though the login response reports an expiry.

## What Changes

### Alcance

- Sustituir el token sin firma por un token cuya autenticidad e integridad puedan verificarse.
- Definir y verificar la expiración y los claims de identidad y rol requeridos; rechazar tokens alterados, expirados o con claims requeridos ausentes o incorrectos.
- Mantener la estructura externa documentada de la respuesta exitosa de `POST /auth/login` y alinear la expiración del token con `expiresIn`.
- Acordar y probar el comportamiento de compatibilidad/transición para tokens legados antes del despliegue.
- Mantener H-AUTH-03 pendiente hasta que las pruebas y la decisión de transición respalden su resolución.

### Fuera de alcance

- Cambios en persistencia de usuarios, verificación de contraseñas, CORS o rutas de autenticación.
- Cambios en el request de login, códigos o cuerpos de error, o en los campos y valores documentados de la respuesta, salvo el contenido y formato opaco del campo `token`.
- Cambios en consumidores externos, autorización de otros endpoints o despliegue de una estrategia de transición no acordada.
- Elección de formato, algoritmo, biblioteca o detalles de implementación del token.

### Comportamiento que se preserva

- `POST /auth/login` sigue aceptando el request documentado y no requiere autenticación previa.
- Login válido sigue respondiendo HTTP 200 con `token` (string), `expiresIn` (integer, actualmente 3600), `user.username`, `user.displayName`, `user.role` y `debug` con el valor documentado `authenticated-with-legacy-flow`.
- Los errores de login documentados conservan sus códigos HTTP y cuerpos JSON.
- El cambio intencional respecto de `docs/ENDPOINTS.md` es que `token` deja de tener el formato Base64 legado y pasa a ser un valor verificable; el nombre y tipo externo del campo no cambian. La dependencia de consumidores que decodifiquen el formato anterior requiere una decisión de transición antes del despliegue.

### Resultados verificables

- Un token válido conserva los claims esperados y su validación detecta alteraciones.
- La expiración y los claims faltantes o incorrectos causan rechazo.
- La respuesta HTTP de login conserva la estructura externa documentada.
- El tratamiento de tokens legados queda decidido y probado antes del despliegue.

## Capabilities

### New Capabilities

- `auth/signed-token`: Emisión y validación observable de tokens de autenticación con integridad, claims y expiración verificables.

### Modified Capabilities

- Ninguna.

## Impact

- Componente `auth/`, limitado al comportamiento del token de autenticación y su validación.
- Contrato de `POST /auth/login` documentado en `docs/ENDPOINTS.md`; el formato interno/valor del campo `token` cambia por requerimiento del Issue #24, mientras se preservan los campos externos de éxito.
- H-AUTH-03 en `HALLAZGOS.md`; su estado no se modifica en esta propuesta.
- Los consumidores que dependan del formato legado deben participar en la decisión de transición; no se presupone su compatibilidad.
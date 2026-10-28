# Hallazgos

## auth

Hallazgos ordenados por severidad descendente. Los hallazgos se basan en el
código leído; los riesgos que dependen de consumidores externos o advisories
pendientes se identifican como tales.

| id | componente | hallazgo | tipo | explicacion | evidencia (archivo:linea) | severidad | estado |
|---|---|---|---|---|---|---|---|
| H-AUTH-02 | auth | Contraseñas guardadas y sembradas en texto plano. | Vulnerabilidad | Resuelto según estrategia de migración acordada: nuevas cuentas de demostración se siembran con PBKDF2-HMAC-SHA256 y salt aleatorio; credenciales planas heredadas se migran al primer login válido. Las filas heredadas aún no autenticadas permanecen en claro hasta su login. No se añadió dependencia. | `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:10-83`; `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:19-26,49-110,132-145`; `auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java:72-120`; `auth/src/test/java/com/bcp/workshop/auth/PasswordHasherTest.java:8-27` | alta | Resuelto |
| H-AUTH-01 | auth | Credenciales concatenadas en la consulta de login; no se usan parámetros JDBC. | Vulnerabilidad | Resuelto: username se enlaza en `PreparedStatement`; la contraseña se verifica fuera del texto SQL y la migración usa parámetros. | `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:27-29,91-93,140-143`; `auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java:28-70` | alta | Resuelto |
| H-AUTH-03 | auth | Token construido mediante Base64 con secreto embebido, sin firma criptográfica. | Vulnerabilidad | Pendiente de cierre de integración: auth ahora emite y verifica JWT HS256 con `sub`, `role`, `iat` y `exp`; no se validaron consumidores ni la transición de tokens previos. El despliegue queda bloqueado hasta esa validación. | `auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java:23-100`; `auth/src/test/java/com/bcp/workshop/auth/JwtTokenServiceTest.java:25-147`; registro en `PLAN.md` | alta | Pendiente |
| H-AUTH-08 | auth | Fallos de persistencia durante login se convierten en credenciales inválidas. | Bug | Candidato: `findByCredentials` captura excepciones de BD y retorna `null`; el servicio convierte ese resultado a HTTP 401. Una indisponibilidad puede presentarse al cliente como credenciales incorrectas y ocultar la diferencia operacional. | `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:89-109`; `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:27-30`; contrato en `docs/ENDPOINTS.md:122-142` | alta | CANDIDATO |
| H-AUTH-04 | auth | La respuesta de error 500 expone el mensaje y la clase de la excepción. | Vulnerabilidad | Resuelto: el cliente recibe un mensaje genérico sin campo de clase; el detalle se registra en el log del servidor. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:19,26-32`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:78-97` | alta | Resuelto |
| H-AUTH-05 | auth | Configuración de CORS permite un patrón de origen amplio y el secreto del token está fijo en código. | Vulnerabilidad | Resuelto: CORS queda limitado al origen configurable y `AUTH_TOKEN_SECRET` se provee desde configuración externa en dev/prod; tests usan valores ficticios. El secreto se valida para el uso con HS256; véase H-AUTH-03 para el cierre pendiente de consumidores. | `auth/src/main/resources/application.properties:5,9-11`; `auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java:31-36`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:147-169` | media | Resuelto |
| H-AUTH-06 | auth | `AuthResource.login` concentra validación, acceso a persistencia, token y armado de respuesta. | Refactor | Resuelto: la ruta delega flujo y validación a `LoginService` con outcomes tipados; el recurso mapea outcomes a respuestas HTTP y DTOs. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26-46`; `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:22-65`; `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3`; `auth/src/test/java/com/bcp/workshop/auth/LoginServiceTest.java:11-65` | media | Resuelto |
| H-AUTH-07 | auth | Versiones candidatas de dependencias requieren evaluación de advisories; no se confirmó un CVE. | Sugerencia | El reto señala Quarkus 3.27.4 y Commons Text 1.9 para investigar. Las versiones están declaradas, pero la presencia de esas versiones por sí sola no demuestra una vulnerabilidad explotable ni su aplicabilidad. | `auth/pom.xml:13,49-50` | media | Pendiente |
| H-AUTH-09 | auth | Login no define un límite de longitud para username o password. | Vulnerabilidad | Candidato: la validación solo descarta valores nulos o en blanco; para un usuario existente, una contraseña de tamaño arbitrario llega al cálculo PBKDF2. Entradas grandes podrían incrementar CPU y memoria por solicitud. La existencia de límites en infraestructura externa no se comprobó. | `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:36-45`; `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:89-99`; `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:76-78`; `docs/ENDPOINTS.md:27-34` | media | CANDIDATO |
| H-AUTH-10 | auth | No se encontró control de frecuencia de intentos de login en el componente. | Vulnerabilidad | Candidato: el recurso admite el flujo repetido y cada solicitud de credenciales llega al servicio y a persistencia; una automatización podría probar contraseñas reiteradamente. No se comprobó si existe mitigación en proxy o infraestructura externa, por lo que el riesgo efectivo queda pendiente. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:24-31`; `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:22-30`; búsqueda sin coincidencias en `auth/src/main/java` y `auth/src/main/resources` para `rate.?limit|throttl|attempt|lockout` | media | CANDIDATO |

### Candidatos de discovery — auth (pendientes de revisión humana)

#### H-AUTH-08 — Errores de persistencia mapeados a 401

- **Estado:** CANDIDATO
- **Evidencia:** `LegacyDatabase.findByCredentials` captura cualquier excepción y retorna `null` (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:89-109`); `LoginService.login` convierte `null` en credenciales inválidas (`auth/src/main/java/com/bcp/workshop/auth/LoginService.java:27-30`). El contrato documenta 401 para credenciales inválidas y 500 para excepciones (`docs/ENDPOINTS.md:122-142`).
- **Impacto:** una falla de conexión o consulta puede parecer al usuario una contraseña incorrecta y a clientes/monitoreo una respuesta de autenticación inválida, dificultando distinguir indisponibilidad.
- **Prioridad propuesta:** P2 — verificar con el responsable del contrato y la operación antes de priorizar.
- **Contrato potencialmente afectado:** `POST /auth/login`; distinción entre HTTP 401 (credenciales inválidas) y HTTP 500 (error interno).
- **Cómo validarlo:** con un proveedor JDBC de prueba que lance `SQLException`, ejecutar login a nivel HTTP y comprobar que el resultado corresponde al contrato de error interno; comprobar por separado que credenciales incorrectas continúan devolviendo 401. No ejecutado en este discovery.

#### H-AUTH-09 — Sin límite de longitud visible para credenciales

- **Estado:** CANDIDATO
- **Evidencia:** `LoginService.validateRequest` solo valida nulo/blanco (`auth/src/main/java/com/bcp/workshop/auth/LoginService.java:36-45`). La contraseña para usuario encontrado se entrega a PBKDF2 (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:89-99`; `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:76-78`). El request documenta los campos pero no una longitud máxima (`docs/ENDPOINTS.md:27-34`).
- **Impacto:** solicitudes con valores excesivamente grandes pueden consumir recursos adicionales; no se ha medido el efecto ni descartado un límite del servidor/proxy.
- **Prioridad propuesta:** P2 — requiere medir límites reales y acordar límites de negocio.
- **Contrato potencialmente afectado:** `POST /auth/login`; añadir límites podría cambiar qué entradas son aceptadas y la respuesta a credenciales fuera de esos límites.
- **Cómo validarlo:** en entorno local de prueba, medir latencia y memoria para username/password en varios tamaños y confirmar límites efectivos del servidor; después de acordar un máximo, probar el valor límite y el siguiente tamaño rechazado. No ejecutado en este discovery.

#### H-AUTH-10 — Sin rate limiting visible en auth

- **Estado:** CANDIDATO
- **Evidencia:** cada request aceptado pasa de `AuthResource.login` a `LoginService.login` y al repositorio (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:24-31`; `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:22-30`). La búsqueda `rg -ni "rate.?limit|throttl|attempt|lockout" auth/src/main/java auth/src/main/resources` no encontró coincidencias. Esto no descarta controles externos.
- **Impacto:** si no hay controles en otra capa, intentos automatizados repetidos podrían aumentar la probabilidad de adivinar credenciales y consumir recursos del servicio.
- **Prioridad propuesta:** P2 — condicional a confirmar que no existe protección fuera del componente.
- **Contrato potencialmente afectado:** `POST /auth/login`; cualquier limitación futura podría añadir una respuesta de límite de frecuencia no contemplada en el contrato actual.
- **Cómo validarlo:** revisar con el responsable de despliegue si hay limitación en proxy/gateway; realizar una prueba controlada con solicitudes ficticias y medir si hay bloqueo, demora o respuesta diferenciada. No ejecutado en este discovery.

### Contrato observado de `POST /auth/login`

El contrato detallado se mantiene en `docs/ENDPOINTS.md`; el siguiente resumen
registra el comportamiento observado y sus límites:

- Ruta y método: `POST /auth/login`; consume y produce JSON.
  Evidencia: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:13-24`.
- Cuerpo: objeto `LoginRequest` con campos públicos `username`, `password` y
  `client`. Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-6`.
- Validación: solicitud nula, usuario vacío o contraseña vacía producen HTTP
  400. Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:36-46`;
  el recurso mapea a HTTP 400 en `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:35-38`.
- Persistencia: busca por usuario en `legacy_users`; los usuarios ficticios
  `student` y `trainer` se inicializan con hashes; las contraseñas planas
  existentes se migran al primer login válido. Evidencia:
  `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:60-85,111-169`.
- Credenciales no coincidentes producen HTTP 401. Éxito produce HTTP 200 con
  `token`, `expiresIn`, `user` y `debug`. Evidencia:
  `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:22-59`.
- El token de éxito es un JWT HS256 con `sub`, `role`, `iat` y `exp`;
  `JwtTokenService` verifica firma y límites temporales. La transición de
  tokens previos y validación de consumidores siguen pendientes:
  `auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java:39-100`.
- Excepción del flujo produce HTTP 500 con `Authentication failed`; el detalle
  se registra en servidor y no se devuelve. Evidencia:
  `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26-32`.

### Pruebas y límites de evidencia

- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` comprueba
  login válido, credenciales incorrectas, validaciones de username/password,
  token JWT verificable, comilla, error 500 genérico y CORS permitido/denegado.
- `auth/src/test/java/com/bcp/workshop/auth/JwtTokenServiceTest.java` verifica
  firma, claims requeridos, expiración, rechazo de payload alterado, algoritmo
  no permitido y clave corta.
- `auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java` registra
  llamadas JDBC para confirmar la consulta parametrizada y verifica la
  siembra de cuentas de demostración con hash, migración de cuenta plana
  existente en login exitoso, autenticación posterior y rechazo de contraseña
  incorrecta.
- `auth/src/test/java/com/bcp/workshop/auth/PasswordHasherTest.java` verifica
  salt distinto, autenticación correcta/incorrecta y hash malformado.
- `auth/src/test/java/com/bcp/workshop/auth/LoginServiceTest.java` cubre
  solicitud nula, validaciones, credenciales no encontradas y mapping DTO.
- Verificación previa a H-AUTH-03: `mvn verify` con cwd `auth/`, BUILD SUCCESS, 18 pruebas,
  0 fallos/errores. JaCoCo: `AuthResource` 12/13 líneas (92.3%),
  `LegacyDatabase` 58/64 (90.6%), `PasswordHasher` 31/34 (91.2%) y
  `LoginService` 23/23 (100%), `LegacyTokenUtil` 4/4 (100%) y `LoginResponse`
  1/1 (100%). `AuthConstants` no contiene líneas ejecutables. Es cobertura por
  clase, no cobertura de líneas de diff.
- Verificación de H-AUTH-03: `mvn verify` con cwd `auth/`, BUILD SUCCESS,
  33 pruebas, 0 fallos/errores. JaCoCo: `JwtTokenService` 42/44 líneas
  (95.5%), `AuthResource` 12/13 (92.3%) y `LoginService` 23/23 (100%).
- La prueba de comilla observó antes del cambio el error SQL capturado que podía
  confundirse con un rechazo normal; la prueba JDBC explícita confirma el
  enlace de parámetro.
- H-AUTH-03 permanece pendiente hasta validar los consumidores externos y
  acordar la transición de tokens previos. El despliegue no está autorizado por
  esta evidencia local.
- La aplicabilidad de advisories de dependencias no se comprobó: Dependency
  Review en el PR #22 falló porque el repositorio no tiene Dependency Graph
  habilitado ([ejecución](https://github.com/user-bcp-student03/ai-labs/actions/runs/40584343315)).
  CodeQL pasó en el PR.

# Hallazgos

## auth

Hallazgos ordenados por severidad descendente. Los hallazgos se basan en el
código leído; los riesgos que dependen de consumidores externos o advisories
pendientes se identifican como tales.

| id | componente | hallazgo | tipo | explicacion | evidencia (archivo:linea) | severidad | estado |
|---|---|---|---|---|---|---|---|
| H-AUTH-02 | auth | Contraseñas guardadas y sembradas en texto plano. | Vulnerabilidad | Resuelto según estrategia de migración acordada: nuevas cuentas de demostración se siembran con PBKDF2-HMAC-SHA256 y salt aleatorio; credenciales planas heredadas se migran al primer login válido. Las filas heredadas aún no autenticadas permanecen en claro hasta su login. No se añadió dependencia. | `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:10-83`; `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:19-26,49-110,132-145`; `auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java:72-120`; `auth/src/test/java/com/bcp/workshop/auth/PasswordHasherTest.java:8-27` | alta | Resuelto |
| H-AUTH-01 | auth | Credenciales concatenadas en la consulta de login; no se usan parámetros JDBC. | Vulnerabilidad | Resuelto: username se enlaza en `PreparedStatement`; la contraseña se verifica fuera del texto SQL y la migración usa parámetros. | `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:27-29,91-93,140-143`; `auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java:28-70` | alta | Resuelto |
| H-AUTH-03 | auth | Token construido mediante Base64 con secreto embebido, sin firma criptográfica. | Vulnerabilidad | Pendiente: se externalizó configuración del secreto (H-AUTH-05), pero por decisión de compatibilidad se conserva el formato Base64 sin firma. No es seguro afirmar resuelto mientras se produzcan tokens sin integridad criptográfica. | `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:12-18`; decisión registrada en `PLAN.md` | alta | Pendiente |
| H-AUTH-04 | auth | La respuesta de error 500 expone el mensaje y la clase de la excepción. | Vulnerabilidad | Resuelto: el cliente recibe un mensaje genérico sin campo de clase; el detalle se registra en el log del servidor. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:19,26-32`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:78-97` | alta | Resuelto |
| H-AUTH-05 | auth | Configuración de CORS permite un patrón de origen amplio y el secreto del token está fijo en código. | Vulnerabilidad | Resuelto: CORS queda limitado al origen configurable y `AUTH_TOKEN_SECRET` se provee desde configuración externa en dev/prod; tests usan un valor ficticio. El token aún no tiene firma; véase H-AUTH-03. | `auth/src/main/resources/application.properties:5,9-11`; `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:12-18`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:99-121` | media | Resuelto |
| H-AUTH-06 | auth | `AuthResource.login` concentra validación, acceso a persistencia, token y armado de respuesta. | Refactor | Resuelto: la ruta delega flujo y validación a `LoginService` con outcomes tipados; el recurso mapea outcomes a respuestas HTTP y DTOs. | `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26-46`; `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:22-65`; `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3`; `auth/src/test/java/com/bcp/workshop/auth/LoginServiceTest.java:11-65` | media | Resuelto |
| H-AUTH-07 | auth | Versiones candidatas de dependencias requieren evaluación de advisories; no se confirmó un CVE. | Sugerencia | El reto señala Quarkus 3.27.4 y Commons Text 1.9 para investigar. Las versiones están declaradas, pero la presencia de esas versiones por sí sola no demuestra una vulnerabilidad explotable ni su aplicabilidad. | `auth/pom.xml:13,49-50` | media | Pendiente |

### Contrato observado de `POST /auth/login`

`docs/ENDPOINTS.md` no existe, así que este resumen deriva del código y no
representa un contrato acordado externamente:

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
- Excepción del flujo produce HTTP 500 con `Authentication failed`; el detalle
  se registra en servidor y no se devuelve. Evidencia:
  `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26-32`.

### Pruebas y límites de evidencia

- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` comprueba
  login válido, credenciales incorrectas, validaciones de username/password,
  comilla, error 500 genérico y CORS permitido/denegado.
- `auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java` registra
  llamadas JDBC para confirmar la consulta parametrizada y verifica la
  siembra de cuentas de demostración con hash, migración de cuenta plana
  existente en login exitoso, autenticación posterior y rechazo de contraseña
  incorrecta.
- `auth/src/test/java/com/bcp/workshop/auth/PasswordHasherTest.java` verifica
  salt distinto, autenticación correcta/incorrecta y hash malformado.
- `auth/src/test/java/com/bcp/workshop/auth/LoginServiceTest.java` cubre
  solicitud nula, validaciones, credenciales no encontradas y mapping DTO.
- Última verificación: `mvn verify` con cwd `auth/`, BUILD SUCCESS, 18 pruebas,
  0 fallos/errores. JaCoCo: `AuthResource` 12/13 líneas (92.3%),
  `LegacyDatabase` 58/64 (90.6%), `PasswordHasher` 31/34 (91.2%) y
  `LoginService` 23/23 (100%), `LegacyTokenUtil` 4/4 (100%) y `LoginResponse`
  1/1 (100%). `AuthConstants` no contiene líneas ejecutables. Es cobertura por
  clase, no cobertura de líneas de diff.
- La prueba de comilla observó antes del cambio el error SQL capturado que podía
  confundirse con un rechazo normal; la prueba JDBC explícita confirma el
  enlace de parámetro.
- H-AUTH-03 permanece pendiente por decisión de conservar compatibilidad con
  el token legado sin firma. La aceptación de ese token sigue sin validación
  criptográfica.
- La aplicabilidad de advisories de dependencias no se comprobó. CodeQL y
  Dependency Review remotos no se ejecutaron.

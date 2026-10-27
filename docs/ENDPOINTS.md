# Contratos de endpoints

Este documento separa el contrato observado en el componente, el comportamiento
descrito por la documentación del workspace y los puntos que siguen pendientes
de confirmación. Los ejemplos de login usan credenciales y secretos ficticios
del laboratorio.

## auth

### `POST /auth/login`

Base URL local del laboratorio: `http://localhost:8080`
(`README.md:44`, `auth/src/main/resources/application.properties:1`).

#### 1. Qué hace

Autentica a una persona usuaria del taller para iniciar sesión y devuelve sus
datos de perfil junto con un token JWT firmado para identificarla.

#### 2. Autenticación requerida

No requiere autenticación previa ni un header `Authorization`. La ruta es
`POST /auth/login` y no declara una restricción de autenticación en
`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:13-26`.
Envía `Content-Type: application/json`.

#### 3. Request

| campo | tipo | obligatorio | descripción |
|---|---|---|---|
| `username` | string | Sí | Nombre de usuario. No puede ser nulo, vacío ni contener solo espacios. |
| `password` | string | Sí | Contraseña. No puede ser nula, vacía ni contener solo espacios. |
| `client` | string | No | Campo aceptado en el cuerpo de solicitud; el flujo de login actual no lo consulta. |

La forma de los campos está definida en `auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-6`; las validaciones están en `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:36-46`. Los campos adicionales desconocidos no provocan rechazo por la configuración de `auth/src/main/resources/application.properties:8`.

Ejemplo JSON completo:

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

#### 4. Response exitosa

**HTTP 200 OK**

| campo | tipo | descripción |
|---|---|---|
| `token` | string | JWT compacto firmado con HS256; contiene `sub`, `role`, `iat` y `exp`. |
| `expiresIn` | integer | Duración declarada del token, en segundos; el valor actual es `3600`. |
| `user` | object | Perfil de la persona autenticada. |
| `user.username` | string | Nombre de usuario. |
| `user.displayName` | string | Nombre visible, escapado para HTML por el servicio. |
| `user.role` | string | Rol almacenado para la cuenta, por ejemplo `STUDENT`. |
| `debug` | string | Indicador fijo del flujo legado: `authenticated-with-legacy-flow`. |

Los campos y tipos corresponden a `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3-4`; el armado está en `auth/src/main/java/com/bcp/workshop/auth/LoginService.java:49-59`, y la duración en `auth/src/main/java/com/bcp/workshop/auth/AuthConstants.java:4-5`.

Ejemplo JSON completo para el perfil de test:

```json
{
  "token": "<JWT firmado HS256 con sub, role, iat y exp>",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

El valor de `token` es un marcador ilustrativo, no un JWT verificable. En una
respuesta real se genera con la clave configurada y la hora de emisión actual.

#### 5. Respuestas de error

Todas las respuestas de error definidas por el recurso de login tienen un cuerpo JSON
con la propiedad `message` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:35-40` y `:26-32`).

**HTTP 400 Bad Request — solicitud nula**

Ocurre si el servicio recibe una solicitud nula.
`auth/src/main/java/com/bcp/workshop/auth/LoginService.java:37-39`.

```json
{
  "message": "Request is null"
}
```

Para enviar una solicitud JSON nula, el cuerpo es el literal `null`; que el
servidor HTTP la convierta en una solicitud nula depende del proveedor JSON y no
se verificó en una llamada HTTP de esta tarea.

**HTTP 400 Bad Request — falta el usuario o está vacío**

```json
{
  "message": "Username is required"
}
```

**HTTP 400 Bad Request — falta la contraseña o está vacía**

```json
{
  "message": "Password is required"
}
```

Las dos condiciones y mensajes están definidos en
`auth/src/main/java/com/bcp/workshop/auth/LoginService.java:40-45`; el mapeo a
HTTP 400 está en `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:35-38`.

**HTTP 401 Unauthorized — credenciales no válidas**

Ocurre cuando no se encuentra una coincidencia para el usuario y la contraseña.
El mensaje incluye el nombre de usuario recibido.
`auth/src/main/java/com/bcp/workshop/auth/LoginService.java:27-30`.

```json
{
  "message": "Invalid user student or password"
}
```

**HTTP 500 Internal Server Error — excepción durante el login**

Ocurre si la ejecución del servicio lanza una excepción. El detalle se registra
en el log del servidor; el mensaje devuelto es genérico.
`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:26-32`.

```json
{
  "message": "Authentication failed"
}
```

**HTTP 403 Forbidden — origen CORS no permitido**

El filtro CORS puede denegar una petición que incluya un origen no incluido en
la configuración. La prueba existente observa HTTP 403 para
`https://untrusted.example` en `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:111-121`;
los orígenes configurados se definen en
`auth/src/main/resources/application.properties:4-7`. El cuerpo de esta
respuesta no está afirmado como contrato: la prueba no verifica ni el formato
ni el contenido del cuerpo. Se debe confirmar antes de que un cliente dependa
de un cuerpo JSON para este error.

Los cuerpos de errores del servidor HTTP para JSON malformado, tipo de
contenido no aceptado, ruta o método inexistentes no están especificados por la
ruta de login ni caracterizados aquí.

#### 6. Notas de seguridad

- La consulta de usuario usa `PreparedStatement` y la contraseña se verifica
  contra un hash PBKDF2-HMAC-SHA256 con salt; las credenciales legadas se
  migran al hash al primer login válido. Evidencia:
  `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:89-110,132-145`
  y `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:10-83`.
- El token es un JWT firmado con HS256. Sus claims son `sub` (username),
  `role`, `iat` y `exp`; la expiración máxima es de 3600 segundos. El verificador
  comprueba algoritmo, firma, claims requeridos, emisión no futura y expiración.
  Evidencia: creación en
  `auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java:39-55`,
  verificación en `auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java:60-100`
  y duración en `auth/src/main/java/com/bcp/workshop/auth/AuthConstants.java:4`.
- JWT firma, pero no cifra, sus claims; no incluir datos sensibles en el token.
- HS256 requiere que los validadores tengan acceso a la misma clave. La clave
  debe ser aleatoria, de alta entropía y protegerse en cada consumidor; esta
  implementación no incluye rotación de claves.
- El código de auth no valida un header Bearer en sus endpoints actuales. No se
  comprobó cómo los consumidores externos interpretan el formato. No desplegar
  el cambio hasta validar esos consumidores y acordar la transición de tokens
  previos; H-AUTH-03 permanece pendiente de ese cierre. El verificador nuevo no
  trata tokens Base64 legados como JWT válido.
- El secreto se lee desde configuración; producción requiere
  `AUTH_TOKEN_SECRET` con al menos 32 bytes UTF-8 y entropía adecuada. Los
  valores de dev/test son ficticios; no incluir un secreto real en el código ni
  en ejemplos. Evidencia:
  `auth/src/main/resources/application.properties:9-11` y
  `auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java:23,31-36`
  (validación de longitud de clave).
- CORS permite por defecto `http://localhost:4200`; puede configurarse con
  `AUTH_CORS_ORIGINS`. Evidencia:
  `auth/src/main/resources/application.properties:4-7`.
- `debug` es un campo de respuesta del flujo legado; no usarlo como prueba de
  autorización ni como sustituto de validar el token.

### `GET /q/health`

#### 1. Qué hace

Permite a una herramienta operativa comprobar la salud del servicio de
autenticación. El workspace lo anuncia como endpoint público de autenticación
en `README.md:44`; la dependencia que proporciona health está declarada en
`auth/pom.xml:38-41`.

#### 2. Autenticación requerida

El README lo describe como público; no se requiere header `Authorization`
según ese contrato documentado. Una infraestructura externa (por ejemplo, un
proxy) podría imponer controles adicionales, lo cual no se verificó.

#### 3. Request

Este endpoint usa `GET` y no requiere cuerpo JSON ni campos de solicitud.

| campo | tipo | obligatorio | descripción |
|---|---|---|---|
| — | — | No aplica | No se define cuerpo de solicitud para esta operación GET. |

Ejemplo de solicitud (sin cuerpo JSON):

```http
GET /q/health HTTP/1.1
Host: localhost:8080
Accept: application/json
```

No hay un ejemplo JSON de request porque la operación no recibe cuerpo.

#### 4. Response exitosa

El contrato de despliegue documenta la URL `http://localhost:8080/q/health`,
pero no especifica el cuerpo. El proveedor SmallRye Health se declara en
`auth/pom.xml:38-41`; no hay una ruta de salud escrita en el código Java de
auth. Una prueba local `@QuarkusTest` observó HTTP 200 y este cuerpo cuando no
hay health checks registrados:

| campo | tipo | descripción |
|---|---|---|
| `status` | string | Estado agregado observado: `UP`. |
| `checks` | array | Lista vacía observada cuando no hay comprobaciones registradas. |

Ejemplo JSON observado:

```json
{
  "status": "UP",
  "checks": []
}
```

La comprobación automatizada valida HTTP 200, `status=UP` y `checks=[]` en
`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`. Este resultado
no caracteriza un entorno con health checks adicionales.

#### 5. Respuestas de error

El código de auth no define respuestas de error para `/q/health`. No hay una
respuesta de error concreta que pueda documentarse o probarse sin inventar el
comportamiento del proveedor ante fallos.

| código | cuándo ocurre | cuerpo de respuesta |
|---|---|---|
| Ninguno definido en el contrato observado | No se caracterizó un caso de health DOWN ni el fallo de una comprobación. | No aplica; status y cuerpo de error están pendientes de definir mediante una comprobación controlada. |

#### 6. Notas de seguridad

El README identifica el endpoint como público; no enviar credenciales ni
tokens en la solicitud. Su disponibilidad pública en un despliegue real,
incluido cualquier control de red o proxy, no se verificó.

### Contrato acordado y preguntas abiertas de auth

- `POST /auth/login` conserva la forma de éxito `token`, `expiresIn`, `user` y
  `debug`, además de sus estados HTTP observados.
- H-AUTH-03 sigue pendiente: el token emitido aún no es criptográficamente
  firmado, pese a que el valor se codifique en Base64.
- El cuerpo de HTTP 403 de CORS no está caracterizado.
- `GET /q/health` se comprobó localmente con `@QuarkusTest`: HTTP 200,
  `status=UP` y `checks=[]` sin comprobaciones registradas. El comportamiento
  cuando la salud está DOWN sigue sin contrato ni prueba. Una prueba
  independiente de servicio en puerto 8080 no se considera evidencia, porque
  `mvn quarkus:dev` se omitió como librería de soporte y no se atribuyó el
  proceso que escuchaba ese puerto.
- La respuesta HTTP 500 del login se prueba directamente en el método del
  recurso con un servicio simulado, no mediante una petición HTTP que provoque
  una falla real de persistencia.

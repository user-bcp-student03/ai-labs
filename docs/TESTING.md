# Pruebas de endpoints

## auth

La matriz corresponde a los endpoints de auth descritos en
[`ENDPOINTS.md`](./ENDPOINTS.md). Los casos marcados como ejecutados se
verificaron en pruebas locales; una respuesta no definida en ese contrato no se
convierte en una expectativa inventada.

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `POST /auth/login` | Login de `student` con contraseña válida | Camino feliz | HTTP 200; conserva los campos del response; el token verifica con HS256 y contiene `sub=student`, `role=STUDENT`, `iat` y `exp` con duración de 3600 segundos. |
| `POST /auth/login` | Usuario omitido | Borde / error 400 | HTTP 400; `{"message":"Username is required"}`. |
| `POST /auth/login` | Usuario compuesto solo por espacios | Borde / error 400 | HTTP 400; `{"message":"Username is required"}`. |
| `POST /auth/login` | Contraseña omitida | Borde / error 400 | HTTP 400; `{"message":"Password is required"}`. |
| `POST /auth/login` | Contraseña compuesta solo por espacios | Borde / error 400 | HTTP 400; `{"message":"Password is required"}`. |
| `POST /auth/login` | Cuerpo JSON `null` | Borde / error 400 | HTTP 400; `{"message":"Request is null"}`. |
| `POST /auth/login` | Contraseña incorrecta | Error 401 | HTTP 401; mensaje `Invalid user student or password`. |
| `POST /auth/login` | Username con comilla | Borde / error 401 | HTTP 401; la entrada se trata como valor literal; no se autentica. |
| `POST /auth/login` | Excepción de servicio simulada en el recurso | Error 500, prueba unitaria de mapeo | El método devuelve HTTP 500 con `{"message":"Authentication failed"}` y omite la clase/detalle simulado. No es una petición HTTP que fuerce un fallo real de base de datos. |
| `POST /auth/login` | CORS con origen configurado | Camino permitido de CORS | El request con `Origin: http://localhost:4200` alcanza el endpoint; HTTP 401 por las credenciales inválidas de prueba y cabecera `Access-Control-Allow-Origin` permitida. |
| `POST /auth/login` | CORS con origen no configurado | Error 403 | HTTP 403 y sin `Access-Control-Allow-Origin`; no se afirma forma de cuerpo, que el contrato deja sin caracterizar. |
| `GET /q/health` | Health sin autenticación, `Accept: application/json` | Camino feliz | HTTP 200, `Content-Type: application/json`, cuerpo `{"status":"UP","checks":[]}` en la configuración de prueba actual. |
| `GET /q/health` | GET sin header `Authorization` | Borde de autenticación | HTTP 200 y `status=UP`; confirma que el endpoint de prueba no requiere dicho header. |
| `GET /q/health` | Estado DOWN / fallo de health check | Pendiente; no hay respuesta de error definida en `ENDPOINTS.md` | No se fija código ni cuerpo. Proponer una prueba cuando auth registre una comprobación cuyo estado se pueda controlar y se acuerde el contrato de fallo. |
| `GET /q/health` | Request de health malformado | No aplica al GET sin cuerpo/campos documentados | No se inventa una respuesta de error ni se agrega una expectativa. |
| Verificador JWT | Payload o firma alterados | Error de validación | `JwtTokenService.verify` rechaza el token con `InvalidTokenException`. |
| Verificador JWT | JWT expirado | Error de validación | `JwtTokenService.verify` rechaza el token expirado. |
| Verificador JWT | Claim `role` ausente o vacío | Error de validación | Rechazo por falta de un claim obligatorio o por valor vacío. |
| Verificador JWT | `iat` en el futuro o duración superior a 3600 segundos | Error de validación | Rechazo de emisión futura o vencimiento más largo que el límite configurado. |
| Verificador JWT | Algoritmo distinto de HS256 | Error de validación | Rechazo; no se acepta un algoritmo alternativo. |
| Verificador JWT | Entrada vacía o malformada | Caso borde / error | Rechazo con excepción genérica de token inválido. |
| Configuración JWT | Clave de firma menor a 32 bytes | Caso borde / error de configuración | El servicio rechaza la clave al inicializarse. |

### Código de pruebas de endpoint

Los siguientes casos están implementados en
`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`. El test 500
invoca el recurso directamente para aislar el mapeo de error; los demás usan
Rest Assured con `@QuarkusTest`.

```java
@Test
void logsInWithWorkshopUser() {
    String token = given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(200)
            .body("user.username", is("student"))
            .body("user.displayName", is("Workshop Student"))
            .body("user.role", is("STUDENT"))
            .body("token", notNullValue())
            .body("expiresIn", is(3600))
            .body("debug", is("authenticated-with-legacy-flow"))
            .extract().path("token");

    JwtTokenService.VerifiedToken claims = tokenService.verify(token);
    assertEquals("student", claims.username());
    assertEquals("STUDENT", claims.role());
    assertEquals(3600, Duration.between(claims.issuedAt(), claims.expiresAt()).getSeconds());
}

@Test
void rejectsMissingUsername() {
    given()
            .contentType("application/json")
            .body("{\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));
}

@Test
void rejectsBlankUsername() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"  \",\"password\":\"Workshop123!\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));
}

@Test
void rejectsMissingPassword() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Password is required"));
}

@Test
void rejectsBlankPassword() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"  \"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Password is required"));
}

@Test
void mapsNullRequestToBadRequest() {
    given()
            .contentType("application/json")
            .body("null")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Request is null"));
}

@Test
void rejectsInvalidCredentials() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"incorrect\",\"client\":\"angular-workshop\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("message", is("Invalid user student or password"));
}

@Test
void treatsQuoteInUsernameAsLiteralInput() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student'\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .body("message", is("Invalid user student' or password"));
}

@Test
void returnsGenericServerErrorWithoutInternalDetails() {
    AuthResource resource = new AuthResource();
    resource.loginService = new LoginService(
            new LegacyDatabase(),
            new JwtTokenService("unit-test-secret-for-HS256-signing-and-verification")
    ) {
        @Override
        public LoginOutcome login(LoginRequest loginRequest) {
            throw new IllegalStateException("simulated-database-detail");
        }
    };
    LoginRequest request = new LoginRequest();
    request.username = "student";
    request.password = "not-a-real-password";

    Response response = resource.login(request);
    Map<?, ?> body = (Map<?, ?>) response.getEntity();

    assertEquals(500, response.getStatus());
    assertEquals("Authentication failed", body.get("message"));
    assertFalse(body.containsKey("exception"));
}

@Test
void allowsConfiguredCorsOrigin() {
    given()
            .header("Origin", "http://localhost:4200")
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"incorrect\",\"client\":\"angular-workshop\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(401)
            .header("Access-Control-Allow-Origin", is("http://localhost:4200"));
}

@Test
void rejectsUnconfiguredCorsOrigin() {
    given()
            .header("Origin", "https://untrusted.example")
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":\"incorrect\",\"client\":\"angular-workshop\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(403)
            .header("Access-Control-Allow-Origin", nullValue());
}

@Test
void reportsHealthyStatusWithoutAuthentication() {
    given()
            .accept("application/json")
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .contentType("application/json")
            .body("status", is("UP"))
            .body("checks.size()", is(0));
}

@Test
void healthEndpointDoesNotRequireAuthorizationHeader() {
    given()
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
}
```

`LoginServiceTest.java` añade pruebas unitarias de solicitud nula, username y
password vacíos, credenciales no encontradas y construcción del DTO. `LegacyDatabaseTest.java`
comprueba la interacción JDBC parametrizada y la migración de contraseña legada;
`PasswordHasherTest.java` verifica hash con salt, coincidencia y rechazos de
hash malformado. Estas pruebas son complementarias, no respuestas HTTP de
endpoint. `JwtTokenServiceTest.java` comprueba emisión y verificación de
claims, firma alterada, expiración, claims obligatorios, algoritmo no permitido,
`iat` futuro, duración máxima, entrada malformada y longitud mínima de clave.

### Comandos y ejecuciones

Los comandos se ejecutaron con cwd `auth/`, usando Java 21 y Maven del entorno.

| comando | directorio | código de salida | resultado |
|---|---|---:|---|
| `mvn test` | `auth/` | 0 | Baseline previo: BUILD SUCCESS; 23 pruebas, 0 fallos, 0 errores. |
| `mvn test` | `auth/` | 0 | Caracterización ampliada del response de login antes del cambio: BUILD SUCCESS; 23 pruebas, 0 fallos, 0 errores. |
| `mvn test` | `auth/` | 0 | Después de implementar JWT: BUILD SUCCESS; 30 pruebas, 0 fallos, 0 errores. |
| `mvn verify` | `auth/` | 0 | Verificación final: BUILD SUCCESS; 33 pruebas, 0 fallos, 0 errores; se generó el reporte JaCoCo. |

La última ejecución de `mvn verify` incluyó 13 pruebas de `AuthResourceTest`,
10 de `JwtTokenServiceTest`, 3 de `LegacyDatabaseTest`, 5 de `LoginServiceTest`
y 2 de `PasswordHasherTest`.

### Cobertura

Método: cobertura de líneas JaCoCo por clase de producción asociada a los
endpoints, leída de `auth/target/site/jacoco/jacoco.xml` generado por
`mvn verify`. No representa la cobertura exclusiva del diff.

| clase de producción | líneas cubiertas / ejecutables | cobertura |
|---|---:|---:|
| `AuthResource` | 12 / 13 | 92.3% |
| `JwtTokenService` | 42 / 44 | 95.5% |
| `LoginService` | 23 / 23 | 100% |

El umbral de referencia es 85%; ambas clases lo superan. Reporte HTML:
`auth/target/site/jacoco/index.html`.

### Fallos, pendientes y pruebas propuestas

- **Fallos:** ninguno en la ejecución final registrada.
- **Pendiente:** no hay un test de error para `GET /q/health`, porque el
  contrato actual no define status o cuerpo para un health DOWN. No se debe
  convertir una expectativa no verificada en contrato.
- **Pendiente:** el HTTP 500 se valida a nivel de método del recurso con
  excepción simulada; falta una prueba de integración HTTP con una falla
  controlada de persistencia.
- **Propuesto:** cuando exista una fuente de health check controlable y se
  acuerde su comportamiento DOWN, agregar test de status y cuerpo exactos.
  Cuando haya un seam de prueba para el servicio/repositorio, agregar prueba
  HTTP de `POST /auth/login` que alcance el 500 sin cambiar el comportamiento
  esperado.
- Se inició `mvn quarkus:dev`, pero Quarkus lo omitió al tratar el módulo como
  librería de soporte; el proceso no se considera arranque del servicio. Una
  petición independiente al puerto 8080 no se atribuye al código del workspace
  porque el proceso que escuchaba ahí no fue identificado. El contrato de
  health documentado arriba se basa exclusivamente en las pruebas
  `@QuarkusTest`.

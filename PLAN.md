# Plan de correcciones

## auth

### Alcance y criterio de selección

Se seleccionaron H-AUTH-01 a H-AUTH-06 por riesgo y alcance. En el estado
actual, H-AUTH-01, H-AUTH-02, H-AUTH-04, H-AUTH-05 y H-AUTH-06 están
`Resuelto` en `HALLAZGOS.md`; H-AUTH-03 permanece `Pendiente` por la decisión
de conservar compatibilidad con tokens legados sin firma. H-AUTH-07 queda
fuera: la presencia de versiones candidatas no confirma advisory aplicable.

`docs/ENDPOINTS.md` no existe. Hasta acordar un contrato, se preservará el
comportamiento observado y probado antes de cada cambio: método/ruta
`POST /auth/login`, validaciones HTTP 400, credenciales inválidas HTTP 401 y,
en éxito, HTTP 200 con las claves `token`, `expiresIn`, `user` y `debug`.
Campos de respuesta y mensajes existentes se caracterizarán antes de
modificarlos. Se acordó permitir CORS desde `http://localhost:4200`, migrar
contraseñas planas a hash al primer login válido y conservar la
compatibilidad del token legado. Esta última decisión impide cerrar H-AUTH-03;
la implementación no valida criptográficamente ni reemplaza el token legado.

Los cambios deben implementarse en etapas revisables, con un método por
responsabilidad, sin duplicar reglas, evitando anidamiento profundo y
reemplazando valores mágicos por constantes nombradas. Se procuró escribir y
ejecutar caracterizaciones antes de cada cambio; toda excepción está declarada
en la sección de desviaciones, sin presentar pruebas propuestas como ejecutadas.
Las aprobaciones documentadas no sustituyen resultados de pruebas.

### H-AUTH-01 — Parametrizar la búsqueda de credenciales APROBADO

- **Hallazgo de HALLAZGOS.md:** credenciales concatenadas a la consulta de
  login; no se enlazan parámetros JDBC.
- **Problema:** los valores de usuario y contraseña forman parte del texto SQL,
  lo que permite que entradas alteren su interpretación.
- **Cambio aplicado:** consultar por username mediante `PreparedStatement`
  con marcador enlazado. La contraseña se compara con el hash seguro o, en
  registros legados, con valor constante-tiempo; la actualización a hash usa
  también parámetros JDBC. No se concatena credencial al SQL.
- **Archivos previstos:** `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`;
  `auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java` (nuevo) y,
  si es necesario para el contrato HTTP, `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- **Caracterización antes de tocar implementación:** cubrir login válido
  (`student` / `Workshop123!`), credenciales inválidas y entrada con comilla
  que debe tratarse como valor literal y no autenticar ni alterar la consulta.
  Añadida prueba JDBC de interacción que comprueba `prepareStatement`,
  `setString(1, username)` y `executeQuery()` sin SQL como argumento. Los
  inserts y la migración también enlazan sus valores con parámetros. La
  conexión se inyecta mediante `ConnectionProvider`, sin dependencia de mocking.
- **Compatibilidad:** conservar la ruta, códigos HTTP, cuerpos JSON observados
  y resultado para credenciales válidas e inválidas.
- **Ejecuciones (auth / H-AUTH-01):**
  - Antes del cambio, `mvn -Dtest=AuthResourceTest test` desde `auth/`: 3
    pruebas pasaron. La prueba de usuario con comilla devolvió 401, pero la
    salida mostró una excepción de sintaxis SQL capturada por el repositorio;
    por sí sola no demuestra parametrización.
  - Antes del cambio, `mvn -Dtest=LegacyDatabaseTest test` desde `auth/`:
    falló como se esperaba porque aún no existía un proveedor de conexión
    inyectable para observar llamadas JDBC.
  - Después del cambio, desde `auth/`,
    `mvn '-Dtest=LegacyDatabaseTest,AuthResourceTest' test`:     4 pruebas pasaron, 0 fallos, 0 errores. La prueba JDBC verifica SQL con
    marcador, `setString(1, username)` y `executeQuery()` sin SQL como argumento.
  - Después del cambio, `mvn verify` desde `auth/`: BUILD SUCCESS, 4 pruebas
    pasaron. JaCoCo reportó para `LegacyDatabase` 24/27 líneas cubiertas
    (88.9%) en esa ejecución histórica.
  - Hubo intentos de Maven desde la raíz, que fallaron con `MissingProjectException`
    porque el POM está en `auth/`; no cuentan como pruebas del componente. Una
    compilación intermedia falló al usar `Supplier` con una operación que lanza
    `SQLException`; se cambió a `ConnectionProvider` y la verificación final
    pasó.
  - CodeQL y Dependency Review remotos: no ejecutados en este recorrido local.
  - Tras H-AUTH-02, el SQL cambió a búsqueda por username y verificación de
    contraseña independiente; H-AUTH-01 sigue parametrizado y fue revalidado
    por `mvn verify`.

### H-AUTH-02 — Almacenar contraseñas con hash lento y salt  APROBADO

- **Hallazgo de HALLAZGOS.md:** contraseñas almacenadas y sembradas en texto
  plano.
- **Problema:** una lectura no autorizada o filtración de SQLite expone las
  contraseñas recuperables.
- **Cambio aplicado:** PBKDF2-HMAC-SHA256 con salt aleatorio para cuentas
  sembradas nuevas y migración de credenciales planas existentes al primer
  login válido. Se parametrizaron los inserts y las actualizaciones y se
  mantuvo comparación de claves derivadas en tiempo constante. No se agregó
  dependencia.
- **Archivos previstos:** `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`;
  posible nueva clase de servicio/verificador bajo
  `auth/src/main/java/com/bcp/workshop/auth/`; pruebas en
  `auth/src/test/java/com/bcp/workshop/auth/` y, si cambia una dependencia,
  `auth/pom.xml`.
- **Caracterización antes del cambio:** `LegacyDatabaseTest` con una SQLite
  efímera y una fila legada; login correcto devolvió el usuario y password
  incorrecta devolvió `null`. Ejecutada antes del cambio de verificación/
  migración al login. La cobertura de migración completa al inicio se añadió
  junto con su implementación.
- **Compatibilidad:** se conserva endpoint y respuesta observada; el hash no se
  devuelve. Las filas planas existentes se migran tras login exitoso y las
  credenciales heredadas siguen funcionando. Las cuentas legadas que no hayan
  iniciado sesión continúan temporalmente en texto plano.
- **Ejecuciones (auth / H-AUTH-02):**
  - Antes del cambio, `mvn '-Dtest=LegacyDatabaseTest,AuthResourceTest' test`
    desde `auth/`: 5 pruebas pasaron. Un primer intento falló por controlador
    SQLite no registrado en el test unitario; se añadió carga explícita del
    driver y la repetición pasó antes de cambiar la verificación de
    credenciales. La prueba específica del sembrado hash se añadió junto con
    su cambio; no se ejecutó contra la implementación antigua.
  - Después de la migración de credenciales existentes, las pruebas de SQLite
    confirmaron migración, login correcto y rechazo de contraseña errónea.
  - Después del cambio de sembrado y migración al login correcto, `mvn verify`
    desde `auth/`: BUILD SUCCESS, 18 pruebas, 0 fallos/errores. JaCoCo:
    `LegacyDatabase` 58/64 líneas (90.6%); `PasswordHasher` 31/34 (91.2%).

### H-AUTH-03 — Reemplazar el token Base64 por token firmado APROBADO

- **Hallazgo de HALLAZGOS.md:** token construido mediante Base64 con secreto
  embebido y sin firma criptográfica.
- **Problema:** Base64 no da integridad ni autenticidad; el contenido puede
  decodificarse y modificarse.
- **Cambio propuesto:** usar un token firmado, con expiración y claims
  explícitos, según `docs/GOOD_PRACTICES.md`. Mantener la respuesta HTTP con
  `token` y `expiresIn`; representar expiración y claims con constantes o
  configuración nombrada, no números mágicos.
- **Archivos previstos:** `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java`
  o su reemplazo; posible clase nueva de seguridad/configuración bajo
  `auth/src/main/java/com/bcp/workshop/auth/`; `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`;
  pruebas bajo `auth/src/test/java/com/bcp/workshop/auth/`. `auth/pom.xml` solo
  si una dependencia de firma se justifica y aprueba.
- **Caracterización antes de tocar implementación:** fijar que un login válido
  devuelve token no nulo, `expiresIn` igual al valor observado y las mismas
  claves/estructura de `user`; fijar el HTTP 401 para credenciales inválidas.
  Añadir pruebas del nuevo formato para verificar firma, expiración y claims
  una vez acordados el formato y los claims.
- **Compatibilidad:** mantener método, ruta y forma externa de la respuesta.
  El formato del token cambiará; `docs/ENDPOINTS.md` falta y no se verificó si
  consumidores dependen de su formato Base64. Coordinar antes de desplegar el
  cambio; la compatibilidad de tokens emitidos previamente queda pendiente.
- **Decisión posterior:** se eligió mantener compatibilidad con tokens legados.
  Esa compatibilidad es incompatible con verificar una firma inexistente; no
  se implementó el token firmado y H-AUTH-03 queda `Pendiente`. La salida del
  generador sigue siendo Base64 y no debe considerarse autenticación segura.

### H-AUTH-04 — Evitar detalles internos en errores HTTP 500 APROBADO

- **Hallazgo de HALLAZGOS.md:** la ruta devuelve el mensaje y la clase de la
  excepción al cliente.
- **Problema:** la respuesta puede revelar detalles de implementación o datos
  internos.
- **Cambio aplicado:** responder HTTP 500 con mensaje genérico y registrar el
  error en el logger del servidor. La prueba simula una excepción interna y
  confirma que su texto/clase no forman parte del cuerpo HTTP.
- **Archivos previstos:** `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`;
  posiblemente clase/helper de manejo de errores bajo
  `auth/src/main/java/com/bcp/workshop/auth/`; pruebas en
  `auth/src/test/java/com/bcp/workshop/auth/`.
- **Caracterización antes de tocar implementación:** simular una excepción en
  el acceso de persistencia y comprobar el HTTP 500, el formato JSON vigente y
  los campos que actualmente se exponen. Esta prueba registra el comportamiento
  actual; tras el cambio, actualizar la expectativa del mensaje a genérico y
  comprobar que no se incluyen texto ni clase de excepción.
- **Compatibilidad:** conservar HTTP 500 y JSON; el valor del mensaje cambia
  intencionalmente para no filtrar detalles. No capturar fallos de forma
  silenciosa.
- **Ejecuciones (auth / H-AUTH-04):**
  - Prueba de caracterización: `AuthResourceTest` simula error de persistencia
    y verifica código/cuerpo; el resultado de una corrida separada antes del
    cambio no quedó registrado.
  - Después del cambio, `mvn verify` desde `auth/`: pasa como parte de 18
    pruebas, 0 fallos/errores. El cuerpo se comprueba sin el detalle simulado;
    ese detalle aparece solo en el log de prueba.

### H-AUTH-05 — Externalizar secreto y restringir CORS APROBADO

- **Hallazgo de HALLAZGOS.md:** CORS configurado con patrón de origen amplio y
  secreto del token embebido en código.
- **Problema:** el origen no queda limitado a los clientes necesarios y el
  secreto es recuperable desde el código o artefacto.
- **Cambio aplicado:** `AUTH_CORS_ORIGINS` configura la allowlist (valor
  predeterminado de laboratorio `http://localhost:4200`). `AUTH_TOKEN_SECRET`
  se lee desde configuración externa para dev/prod y el test usa un valor
  ficticio. No se incorporó secreto real. La decisión de mantener tokens
  legados significa que H-AUTH-03 no consume una firma.
- **Archivos previstos:** `auth/src/main/resources/application.properties`;
  `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` o la clase de
  configuración/token que se acuerde; pruebas bajo
  `auth/src/test/java/com/bcp/workshop/auth/`.
- **Caracterización:** pruebas HTTP para origen permitido y origen no permitido;
  el responsable acordó mantener `http://localhost:4200`. No se agregó prueba
  de arranque sin clave de token.
- **Compatibilidad:** rutas y métodos HTTP permanecen iguales; origen localhost
  permitido; no permitido recibe 403 en la prueba. Producción requiere
  `AUTH_TOKEN_SECRET`. No se verificó despliegue fuera del repositorio.
- **Ejecuciones (auth / H-AUTH-05):**
  - Después del cambio, `mvn verify` desde `auth/`: las pruebas CORS de origen
    permitido/no permitido pasan dentro de 18 pruebas, 0 fallos/errores.
  - No se ejecutó una prueba de arranque con secreto ausente ni se verificaron
    entornos de despliegue.

### H-AUTH-06 — Separar responsabilidades de login APROBADO

- **Hallazgo de HALLAZGOS.md:** `AuthResource.login` mezcla validación,
  consulta, generación de token y construcción de respuesta.
- **Problema:** el anidamiento y acoplamiento dificultan pruebas y elevan el
  riesgo de romper el contrato al cambiar persistencia o seguridad.
- **Cambio aplicado:** `AuthResource` adapta HTTP y `LoginService` valida la
  solicitud, coordina autenticación y construye `LoginResponse`. La persistencia
  permanece en `LegacyDatabase`; no se duplicó la verificación de contraseñas.
- **Archivos previstos:** `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`;
  nuevas clases de servicio, repositorio y DTO bajo
  `auth/src/main/java/com/bcp/workshop/auth/`; pruebas bajo
  `auth/src/test/java/com/bcp/workshop/auth/`.
- **Caracterización antes de tocar implementación:** ampliar
  `AuthResourceTest` para solicitud nula, username vacío, password vacío,
  credenciales válidas e inválidas, y respuesta de error por excepción.
  Verificar códigos HTTP, nombres de campos y estructura JSON observados antes
  de mover lógica.
- **Compatibilidad:** mantener `POST /auth/login`, validaciones HTTP 400,
  autenticación fallida HTTP 401 y forma externa de respuestas 200/500
  acordada para H-AUTH-04. Realizar la separación en cambios pequeños después
  de las caracterizaciones, sin mezclar cambios de contrato.
- **Ejecuciones (auth / H-AUTH-06):**
  - `LoginServiceTest` y `AuthResourceTest` cubren validación, respuestas HTTP,
    login válido/inválido y error interno. No quedó registrado un resultado de
    suite separado antes de la extracción.
  - Después del cambio, `mvn verify` desde `auth/`: BUILD SUCCESS; 18 pruebas,
    0 fallos/errores. JaCoCo: `LoginService` 23/23 (100%) y `AuthResource`
    12/13 (92.3%).

### Dependencias y decisiones pendientes APROBADO

- H-AUTH-07 permanece fuera del alcance de este plan hasta verificar advisories
  y aplicabilidad para las versiones declaradas.
- La allowlist acordada para este laboratorio es `http://localhost:4200`;
  credenciales legadas se migran al login correcto. La transición del token no
  se acordó: conservar el formato sin firma deja H-AUTH-03 pendiente.
- Revisar cualquier dependencia nueva antes de agregarla; no se agrega ninguna
  en esta propuesta.
- Verificación final de auth: `mvn verify` con cwd `auth/` — BUILD SUCCESS,
  18 pruebas, 0 fallos/errores. Cobertura por línea: `AuthResource` 12/13
  (92.3%), `LegacyDatabase` 58/64 (90.6%), `PasswordHasher` 31/34 (91.2%) y
  `LoginService` 23/23 (100%), `LegacyTokenUtil` 4/4 (100%) y
  `LoginResponse` 1/1 (100%). `AuthConstants` no tiene líneas ejecutables.
- CodeQL y Dependency Review remotos: no ejecutados en este recorrido local.
- Desviación: la prueba que asegura que el sembrado genera hashes se añadió
  junto con esa implementación y no se ejecutó contra el código anterior. La
  caracterización previa cubría autenticación con fila plana y su migración al
  login. No se ejecutaron pruebas de arranque sin
  secreto ni pruebas en entornos externos.

### Ejecución de issue — H-AUTH-03 — 2026-10-27

- **Aprobaciones confirmadas por el usuario:** issue H-AUTH-03, HU-01 y
  arquitectura/impacto.
- **Decisiones aplicadas:** JWT HS256 con `sub`, `role`, `iat` y `exp`;
  duración máxima de 3600 segundos; el secreto debe tener al menos 32 bytes
  UTF-8. No se añadió aceptación de tokens legados sin firma. El despliegue
  queda bloqueado hasta validar consumidores externos.
- **Caracterización antes del cambio:** `mvn test`, cwd `auth/`, código 0;
  23 pruebas aprobadas. Se reforzó la prueba funcional de login para fijar
  `user.displayName` y la estructura externa antes de cambiar el token;
  repetición con `mvn test`, cwd `auth/`, código 0, 23 pruebas aprobadas.
- **Implementación:** se reemplazó `LegacyTokenUtil` por
  `JwtTokenService`, que emite y verifica JWT HS256, valida firma y claims
  requeridos y rechaza tokens expirados, `iat` futuro y duración mayor a la
  permitida. `LoginService` conserva la estructura de respuesta. Se agregó
  `nimbus-jose-jwt:10.10` para delegar el formato y la firma JWT a una
  biblioteca JOSE, en vez de implementar criptografía/JWT manualmente.
- **Pruebas posteriores:** `mvn test`, cwd `auth/`, código 0; 30 pruebas,
  0 fallos/errores. `mvn verify`, cwd `auth/`, código 0; 33 pruebas,
  0 fallos/errores y reporte JaCoCo generado.
- **Cobertura JaCoCo por líneas:** `JwtTokenService` 42/44 (95.5%),
  `LoginService` 23/23 (100%) y `AuthResource` 12/13 (92.3%); las clases
  modificadas superan la meta local de 85%.
- **Estado del hallazgo:** H-AUTH-03 no se marca Resuelto: no se comprobaron
  consumidores ni la transición de tokens existentes. La seguridad de emisión
  y verificación se probó localmente; la integración y el despliegue siguen
  pendientes.
- **Riesgos/dependencias pendientes:** CodeQL y Dependency Review remotos no
  se ejecutaron; tampoco se validó la integración de consumidores ni rotación
  de claves. No se añadió `iss` o `aud` porque no formaron parte de las claims
  aprobadas.
- **Incidencia de ejecución:** tres invocaciones de `mvn test` desde la raíz
  fallaron por falta de `pom.xml`; se corrigió el cwd a `auth/` y las
  ejecuciones registradas arriba finalizaron correctamente. No fue un fallo
  del producto.

# Tasks

## 1. Caracterizar el contrato actual antes del cambio funcional

- [x] 1.1 En `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`, conservar la caracterización del login válido (HTTP 200, `user.username` y `token`) y agregar un caso de credenciales ordinarias inválidas (HTTP 401, sin `token`); verificar con `mvn -Dtest=AuthResourceTest test` desde `auth/`.
- [x] 1.2 Agregar casos para username y password en blanco, ambos con HTTP 400; verificar con `mvn -Dtest=AuthResourceTest test` desde `auth/`, antes de modificar el código funcional.

## 2. Añadir la regresión de seguridad para H-AUTH-01

- [x] 2.1 Agregar a `AuthResourceTest` casos con `' OR '1'='1` como username y como password, esperando HTTP 401 y ausencia de `token`; ejecutar `mvn -Dtest=AuthResourceTest test` desde `auth/` y confirmar que los casos detectan el bypass en la implementación vulnerable.

## 3. Corregir la verificación de credenciales

- [x] 3.1 En `LegacyDatabase.findByCredentials`, reemplazar la concatenación de username y password en la consulta por parámetros JDBC para ambos valores; verificar que los casos de inyección y caracterización pasan con `mvn -Dtest=AuthResourceTest test` desde `auth/`.
- [x] 3.2 Ejecutar `mvn test` desde `auth/` y comprobar que las modificaciones de código y pruebas permanecen dentro de `auth/`, sin cambios en `api/` ni `web/`.

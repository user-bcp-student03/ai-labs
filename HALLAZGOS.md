# Hallazgos de discovery

## API

### H-API-01 — Token de identidad forjable con secreto embebido
- Estado: Corregido; pendiente de revisión humana
- Evidencia: `api/app/main.py:14,17,22` define un secreto literal y valida tokens decodificando Base64 y comparando el secreto y la expiración; no hay firma criptográfica. `api/tests/test_api.py:10` también construye el token con ese valor.
- Impacto: quien conozca el secreto del código puede fabricar tokens con usuario y rol arbitrarios y acceder a las rutas protegidas.
- Prioridad propuesta: Alta.
- Contrato potencialmente afectado: autenticación y autorización de todos los endpoints que dependen de `legacy_identity`.
- Cómo validarlo: cubierto por `test_rejects_forged_and_tampered_tokens`, `test_rejects_expired_signed_token` y `test_requires_token_secret` en API; `AuthResourceTest` verifica la firma emitida por auth.
- Evidencia posterior: `python tools/lab.py test api` — 7 passed; cobertura total 92.06%, gate de 85% superado. `python tools/lab.py test auth` — Maven `verify` exitoso, 1 test passed; JaCoCo registra `LegacyTokenUtil` con 8/8 líneas cubiertas.

### H-API-02 — SQL construido por concatenación en endpoints de tareas
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:70` concatena `owner` en el SELECT de `/api/tasks`; `api/app/main.py:84` concatena campos del payload en el INSERT de `/api/tasks`; `api/app/main.py:93` concatena `owner` en el SELECT de `/api/summary`.
- Impacto: entradas controlables pueden alterar la consulta, revelar registros de otros propietarios o modificar el resultado de operaciones sobre tareas.
- Prioridad propuesta: Alta.
- Contrato potencialmente afectado: integridad y confidencialidad de `/api/tasks` y `/api/summary`, además de sus formatos de respuesta esperados por clientes.
- Cómo validarlo: agregar pruebas de caracterización con valores que incluyan comillas y operadores SQL en `owner`, `title` y otros campos; verificar que se tratan como datos literales y que no cambian el alcance ni el resultado de la consulta. Confirmar que las consultas usan parámetros.

### H-API-03 — El cliente puede consultar o asignar tareas a otros propietarios
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:69-70` acepta el parámetro `owner` sin ligarlo a `identity["username"]`; `api/app/main.py:83-84` acepta `owner` desde el payload al crear una tarea. La identidad sí se obtiene como dependencia en ambas rutas.
- Impacto: un usuario autenticado puede leer el listado o resumen de otro propietario y crear tareas atribuidas a otra identidad, si no existe una política de roles adicional aplicada en otro punto.
- Prioridad propuesta: Alta.
- Contrato potencialmente afectado: aislamiento de datos y autorización ligada a la identidad en `/api/tasks` y `/api/summary`.
- Cómo validarlo: con dos usuarios de prueba, solicitar listado/resumen con `owner` ajeno e intentar crear una tarea indicando ese propietario; contrastar el comportamiento con la política de autorización acordada y probar explícitamente cualquier excepción administrativa.

### H-API-04 — Nombre de archivo de carga usado directamente como ruta
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:108-110` forma el destino con `Path(selected_uploads) / str(file.filename)` y escribe los bytes sin normalizar ni validar el nombre.
- Impacto: un nombre de archivo con componentes de ruta puede permitir escribir fuera del directorio de cargas, dentro de los permisos del proceso.
- Prioridad propuesta: Alta.
- Contrato potencialmente afectado: almacenamiento aislado de `/api/requirements` y contenido del filesystem accesible al servicio.
- Cómo validarlo: probar con nombres que contengan separadores y segmentos de navegación en un directorio temporal; verificar que ningún archivo se crea fuera del directorio de carga y que los nombres no aceptados producen un error controlado.

### H-API-05 — Carga de archivos sin límites de tamaño ni política de tipo
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:110` lee el archivo completo en memoria y `api/app/main.py:111` lo persiste sin comprobar tamaño o extensión; `api/tests/test_api.py:41-52` solo cubre la carga de un archivo pequeño de texto.
- Impacto: archivos grandes pueden consumir memoria y espacio de almacenamiento; tipos inesperados pueden quedar almacenados pese a que el endpoint representa la carga de requisitos.
- Prioridad propuesta: Media.
- Contrato potencialmente afectado: disponibilidad y política de archivos aceptados por `/api/requirements`.
- Cómo validarlo: añadir pruebas de límites para tamaño y extensiones/tipos permitidos, incluyendo un archivo superior al máximo definido; verificar rechazo controlado sin persistencia parcial ni consumo ilimitado de memoria.

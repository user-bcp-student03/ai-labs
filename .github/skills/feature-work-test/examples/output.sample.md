# Feature Intake -> Spec Checklist

## 1. Summary
Automatizar una tarea diaria de pruebas que genere un listado del estado de salud de las aplicaciones de los grupos A, B, C y D en desarrollo y certificación. El inventario, las reglas de salud y el detalle de entrega del reporte requieren definición antes de implementar.

## 2. Scope
- **Dentro del alcance**
  - Ejecutar diariamente los chequeos de salud que se acuerden para las aplicaciones incluidas en el inventario aprobado.
  - Organizar el reporte por los grupos A, B, C y D y por los ambientes de desarrollo y certificación.
  - Registrar resultados y hacer explícitas las aplicaciones sin resultado y las ejecuciones fallidas o no realizadas.
- **Fuera del alcance**
  - Corregir o modificar aplicaciones o ambientes a partir de los resultados.
  - Definir sin aprobación el inventario, los umbrales de salud, el horario o el destino del reporte.

## 3. Supuestos
- La periodicidad requerida es diaria.
- Los grupos requeridos son A, B, C y D; los ambientes requeridos son desarrollo y certificación.
- No se proporcionaron herramientas, horario, zona horaria, inventario ni reglas de clasificación; no se presuponen.

## 4. Decisiones técnicas por tomar (pendientes o resueltas)
- **Pendiente:** inventario de aplicaciones y correspondencia con cada grupo.
- **Pendiente:** chequeos, umbrales, tiempos de espera y reglas para estados saludables, no saludables y sin resultado.
- **Pendiente:** hora y zona horaria de ejecución diaria.
- **Pendiente:** formato, destino, destinatarios y período de retención del reporte.
- **Pendiente:** manejo y notificación de errores de ejecución y de chequeos.
- **Resuelto por el requerimiento:** incluir los grupos ABCD, los ambientes de desarrollo y certificación y una periodicidad diaria.

## 5. Riesgos y mitigaciones
- **Clasificación incorrecta o cobertura incompleta:** acordar y versionar el inventario por grupo, los ambientes aplicables y las reglas de salud antes de activar la automatización.
- **Ejecución omitida confundida con éxito:** distinguir resultados saludables de errores, datos ausentes y ejecuciones no realizadas; verificar también que la tarea programada se ejecute.
- **Exposición de información interna en el reporte:** limitar el contenido a los campos autorizados y entregar el reporte únicamente a un destino aprobado; nunca incluir secretos ni credenciales.

## 6. Criterios de aceptación (medibles)
- La tarea se ejecuta una vez por día según el horario y la zona horaria que se aprueben.
- Cada ejecución produce un reporte con secciones o agrupaciones identificables para A, B, C y D y para desarrollo y certificación; las combinaciones sin aplicaciones registradas se identifican explícitamente.
- Por cada aplicación incluida en el inventario aprobado, el reporte identifica grupo, ambiente, estado según las reglas acordadas y evidencia o motivo del resultado.
- Una falla, ausencia de datos o ejecución no realizada nunca se presenta como un estado saludable; se identifica como tal en el reporte.
- El reporte no expone secretos, credenciales ni datos fuera de los campos autorizados.

## 7. Plan de validación
- **Pruebas unitarias:** validar la asignación de aplicaciones a grupos/ambientes, la traducción de resultados a estados y la representación de errores o datos ausentes.
- **Pruebas funcionales:** comprobar la ejecución diaria con el horario aprobado; contrastar el reporte con el inventario de prueba para A-D en desarrollo y certificación; verificar escenarios saludables, fallidos y sin resultado.
- **Seguridad (comprobaciones mínimas):** confirmar acceso autorizado a los ambientes y al destino del reporte, y comprobar que el reporte y los registros no incluyan secretos ni credenciales.
- **Evidencia requerida:** configuración de la programación aprobada, resultados de pruebas, un reporte de ejemplo con datos de prueba y evidencia de una ejecución fallida o sin resultado.
- **Automatización:** validar la periodicidad diaria, la cobertura de grupos y ambientes, los estados reportados y el tratamiento de fallas de ejecución.

## 8. Tareas sugeridas por rol
- **Chapter/PO**
  - Confirmar el inventario de aplicaciones y su asignación a A, B, C y D.
  - Acordar los ambientes aplicables por aplicación.
  - Aprobar los destinatarios, el formato y el contenido del reporte.
- **Tech Lead**
  - Definir la arquitectura y el mecanismo de programación dentro del stack permitido.
  - Acordar el contrato de entrada y salida del reporte y la trazabilidad por ejecución.
  - Revisar controles de acceso, manejo de fallas y operación diaria.
- **Backend**
  - Implementar la ejecución de los chequeos aprobados en los ambientes autorizados.
  - Generar el reporte organizado por grupo y ambiente, incluyendo resultados no saludables y ausentes.
  - Proteger la configuración sensible y evitar que secretos aparezcan en reportes o registros.
- **Frontend**
  - Pendiente de confirmar si se requiere una interfaz; no se solicitó una en el alcance actual.
- **QA**
  - Diseñar casos para cada grupo/ambiente y para resultados saludables, fallidos y ausentes.
  - Validar la ejecución programada y el contenido del reporte contra el inventario aprobado.
  - Adjuntar evidencia de pruebas y verificar que una ejecución fallida no se informe como saludable.
- **Cyber**
  - Revisar autorización para acceder a los ambientes de desarrollo y certificación.
  - Verificar que el destino y los destinatarios del reporte estén aprobados.
  - Revisar el reporte y los registros para detectar exposición de secretos o datos no autorizados.

## 9. Preguntas abiertas
- ¿Cuál es el inventario de aplicaciones y cómo se asigna a cada grupo ABCD?
- ¿Qué aplicaciones deben probarse en desarrollo, certificación o ambos?
- ¿Qué chequeos, umbrales y reglas definen el estado de salud?
- ¿A qué hora y en qué zona horaria se ejecuta la tarea?
- ¿Qué formato, campos, destino, destinatarios y retención requiere el reporte?
- ¿Cómo se deben notificar o escalar fallas de ejecución y resultados no saludables?
- ¿Qué stack y mecanismo de automatización están permitidos?
- ¿Se requiere una interfaz para consultar reportes o basta con el reporte entregado?
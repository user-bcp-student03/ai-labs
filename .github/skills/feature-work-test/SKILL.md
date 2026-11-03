---
name: feature-intake-spec-checklist
description: Estandariza la definición de funcionalidades y automatizaciones de pruebas, incluidas tareas diarias que reportan la salud de aplicaciones por grupo y ambiente.
---

# Skill: Feature Intake -> Spec Checklist

## 1) Metadata
- **Skill ID:** feature-intake-spec-checklist
- **Version:** 0.1.0
- **Purpose:** Estandarizar la definición de funcionalidades antes de la implementación generando un paquete mínimo trazable que cubra alcance, decisiones, riesgos, aceptación y validación. Para automatizaciones de pruebas, especificar la periodicidad, las aplicaciones y grupos, los ambientes, los chequeos y el reporte esperado.
- **Audience (roles):** Chapter/PO, Tech Lead, Backend, Frontend, QA, Cyber
- **When to use:** Antes de crear tareas de implementación o abrir un PR significativo, y al definir automatizaciones de pruebas recurrentes que deban generar reportes de salud de aplicaciones.
- **When NOT to use:** Incidentes P1 de producción, cambios triviales como typos o tareas sin impacto funcional.
- **Sensitive inputs:** No incluyas datos reales de clientes, credenciales, secretos, tokens ni URLs internas no autorizadas.
- **Expected output:** Markdown estructurado mediante la sección Output a continuación.

## 2) Inputs
Proporciona la información en este formato:

- **Business context (brief):**
- **Feature request / user story:**
- **Expected impact (metric or outcome):**
- **Scope (in/out):**
- **Known technical constraints:** (stack, standards, performance, availability)
- **Known risks:** (security, compliance, stability)
- **Dependencies:** (services, teams, data)
- **Team Definition of Done (DoD):** (if available)
- **Automation cadence and schedule:** (for example, daily; include time and time zone only if defined)
- **Application inventory and group mapping:** (applications assigned to each group, such as ABCD)
- **Target environments:** (for example, development and certification)
- **Health checks and status rules:** (checks, thresholds, and how healthy, unhealthy, unknown, or not-run results are determined)
- **Report requirements:** (required fields, format, destination, recipients, and retention, if known)
- **Failure handling:** (what to do when a check or the scheduled run fails, if defined)

## 3) Output
Devuelve **solo** un documento Markdown con estas secciones y bullets:

1. **Summary**
2. **Scope**
    - Dentro del alcance
    - Fuera del alcance
3. **Supuestos**
4. **Decisiones técnicas por tomar (pendientes o resueltas)**
5. **Riesgos y mitigaciones**
6. **Criterios de aceptación (medibles)**
7. **Plan de validación**
    - Pruebas unitarias
    - Pruebas funcionales
    - Seguridad (comprobaciones mínimas)
    - Evidencia requerida (qué adjuntar)
    - Para automatizaciones: validación de periodicidad, cobertura de grupos/ambientes, estados reportados y fallas de ejecución.
8. **Tareas sugeridas por rol**
    - Chapter/PO
    - Tech Lead
    - Backend
    - Frontend
    - QA
    - Cyber
9. **Preguntas abiertas**

## 4) Constraints
- No inventes requisitos: si falta información, inclúyela bajo **Preguntas abiertas**.
- Para tareas programadas, no inventes horario, zona horaria, inventario de aplicaciones, asignación a grupos, umbrales de salud, destino del reporte ni política de reintentos; identifícalos como pendientes cuando no estén definidos.
- Un chequeo fallido, una ejecución omitida o un resultado sin datos no debe representarse como una aplicación saludable.
- No propongas herramientas fuera del stack permitido.
- No incluyas datos sensibles ni secretos.
- Los criterios de aceptación deben ser verificables; no escribas “mejorar el rendimiento” sin una métrica.
- El plan de validación debe incluir al menos un elemento de evidencia concreto, como salida de pruebas, un checklist o un archivo actualizado.
- En un reporte de salud, especifica cómo se listan las aplicaciones por grupo y ambiente y qué evidencia acompaña cada estado; aclara las decisiones pendientes antes de fijar estados o umbrales.
- Mantén la salida en un máximo de 1-2 páginas para que siga siendo usable.

## 5) Validation Rules
La salida es válida si:
- Incluye todas las secciones definidas en Output.
- Tiene al menos 3 criterios de aceptación medibles.
- Tiene al menos 3 tareas para cada rol central: Tech Lead, Backend y QA, o explica por qué un rol no aplica.
- Incluye al menos 2 riesgos con mitigaciones.
- Si la solicitud es una automatización diaria de salud, incluye criterios verificables para la periodicidad diaria y para el reporte por cada grupo y ambiente solicitados, sin asumir valores aún no acordados.
- No contiene datos sensibles ni secretos.
- No contiene supuestos no declarados.

## 6) Evidence
Adjunta o referencia estos elementos en el PR o entrega:
- `SKILL.md` versionado.
- `examples/input.sample.md`.
- `examples/output.sample.md`.
- Tag de versión para el Skill, como `skill-feature-intake-v0.1.0`.
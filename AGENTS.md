# AGENTS.md — discovery-server

Servidor de descubrimiento Eureka de Convivo (Spring Cloud Netflix Eureka Server). Registro y localización de los demás microservicios del workspace.

**Para el agente que trabaje en este microservicio:**

- Sin emojis en código, PR, docs generadas ni output — usar solo como último recurso si no existe alternativa real.
- En commits rige Gitmoji (§11.2) — ahí el emoji es obligatorio por convención.
- Nada de solución genérica de tutorial. Cada decisión responde a Convivo, no a un boilerplate de curso.
- **Nota de dominio**: el paquete Java es `com.convivo.discovery_server`.

## 0. Jerarquía de reglas

1. Seguridad y corrección — nunca se sacrifican por ninguna otra regla.
2. Convenciones del proyecto (stack, estilo, arquitectura) — se siguen salvo instrucción explícita en contrario.
3. Minimalismo (Ponytail) — se aplica solo después de satisfacer 1 y 2.

## 1. Resumen del proyecto

`discovery-server` es el servidor de descubrimiento de Convivo (Netflix Eureka vía Spring Cloud). Los demás microservicios se registran acá para descubrirse entre sí sin URLs hardcodeadas. Puerto `8761`. Importa configuración de `config-server` de forma **opcional** (`optional:configserver:http://localhost:8888`) — arranca igual si config-server no está disponible. No expone lógica de negocio propia.

## 2. Stack técnico

- Lenguaje: Java 21
- Framework: Spring Boot **4.1.1** + Spring Cloud **2025.1.1** (`spring-cloud-starter-netflix-eureka-server`, `spring-cloud-starter-config`) — mismas versiones que `config-server`, unificadas 2026-09-03.
- Build: Maven (wrapper `mvnw`/`mvnw.cmd`)
- Tests: JUnit 5 (`spring-boot-starter-test`)
- Contenedores: Docker (multi-stage, `eclipse-temurin:21-jre-alpine` en runtime) + docker-compose con healthcheck

## 3. Estructura del proyecto

```text
src/main/java/com/convivo/discovery_server/
  DiscoveryServerApplication.java   # @SpringBootApplication + @EnableEurekaServer
src/main/resources/
  application.yml                   # nombre de app + import opcional de config-server
src/test/java/com/convivo/discovery_server/
  DiscoveryServerApplicationTests.java   # smoke test de contexto
dockerfile                          # build multi-stage
docker-compose.yml                   # build local (sin volumen), con healthcheck sobre /actuator/health
```

Sin `src/main/resources/config/` — a diferencia de `config-server`, este servicio no sirve configuración a terceros, la consume.

## 4. Comandos

```bash
# instalar / compilar
./mvnw clean install -DskipTests

# test completo
./mvnw test

# test acotado a una clase
./mvnw test -Dtest=DiscoveryServerApplicationTests

# levantar local
./mvnw spring-boot:run

# levantar con docker-compose (build local, sin volumen montado — a diferencia de config-server)
docker compose up --build

# build de la imagen manualmente
docker build -t discovery-server .
```

Todo comando de arriba es ejecutable tal cual desde la raíz de `discovery-server/`.

## 5. Estilo de código

`(no aplica: sin código propio más allá de la clase de arranque — discovery-server no tiene controladores, servicios ni repositorios propios, toda su función la resuelve spring-cloud-starter-netflix-eureka-server por configuración)`.

## 6. Disciplina anti-sobreingeniería (Ponytail)

Configuración global del agente — no duplicar aquí. Aplica a código nuevo a escribir, no autoriza podar documentación existente.

## 7. Pruebas

`(no aplica cobertura formal: servicio de descubrimiento sin lógica de negocio propia)`. Único test existente: `DiscoveryServerApplicationTests` (smoke test de contexto — `contextLoads()`). Framework: JUnit 5 vía `spring-boot-starter-test`. Ubicación: `src/test/java/com/convivo/discovery_server/`.

## 8. Métricas de claridad

`(no aplica: sin lógica de negocio propia que justifique métricas formales)`.

## 9. Procedimientos QA

Checklist pre-entrega: build en verde (`./mvnw clean install`), healthcheck de `docker-compose.yml` en verde (`curl -f http://localhost:8761/actuator/health`), sin secrets hardcodeados, documentación actualizada si cambia la estrategia de import de config (`optional:configserver:...`).

| Severidad | Acción |
| --- | --- |
| Crítico | bloquea el merge |
| Mayor | corregir antes del merge |
| Menor | issue post-merge |

## 10. Seguridad

**Hallazgo real, sin corregir en este cambio**: `application.yml` local no configura autenticación, pero `config-server/src/main/resources/config/discovery-server.yml` (que este servicio importa vía `optional:configserver:...`) sí define `spring.security.user.name: admin` / `password: admin123` — una credencial débil hardcodeada en el repo. El problema real no es solo la contraseña: `pom.xml` **no incluye `spring-boot-starter-security`** como dependencia, así que Spring Security nunca se autoconfigura y esas credenciales no protegen nada — el dashboard/API de Eureka en el puerto `8761` queda abierto igual. Es un control roto (configurado pero inerte), no solo ausente. Cualquiera con red a ese puerto ve el registro completo de servicios (nombres, IPs, puertos) y puede registrar/desregistrar instancias falsas. Mismo tipo de brecha (OWASP A02) que la documentada en `config-server/AGENTS.md` §10.

El `config.import` con prefijo `optional:` es intencional, no un descuido: el servicio debe poder arrancar sin `config-server` disponible (evita un ciclo de arranque circular entre ambos) — documentarlo como comportamiento esperado si se audita.

**OWASP Top 10:2025 — alcance real en este proyecto:**

- **A01 Control de acceso roto**: sin control de acceso implementado sobre el dashboard/API de Eureka — ver hallazgo arriba.
- **A02 Configuración insegura**: ver hallazgo arriba; sin HTTPS configurado (HTTP plano en `8761`).
- **A03 Fallos de cadena de suministro**: dependencias resueltas vía `pom.xml` con `spring-cloud-dependencies` como BOM; revisar con `dependency-audit` antes de cada release.
- **A04 Fallos criptográficos**: `(no aplica: sin criptografía propia)`.
- **A05 Inyección**: `(no aplica: sin DB relacional, sin queries)`.
- **A06 Diseño inseguro**: `(no aplica: servicio de descubrimiento sin flujo de negocio propio)`.
- **A07 Fallos de autenticación**: `(no aplica: sin autenticación de usuarios — ver A01 para el control de acceso al servicio en sí)`.
- **A08 Fallos de integridad**: `(no aplica: sin deserialización de input externo)`.
- **A09 Fallos de logging**: sin logging de eventos de seguridad configurado (mismo estado que A01).
- **A10 Condiciones excepcionales**: comportamiento por defecto de Spring Boot.

`(no aplica: sin superficie LLM)`.

## 11. Commits y PR

Conventional Commits v1.0.0 + Gitmoji. Formato: `:emoji: <tipo>(<alcance>)?(!)?: <sujeto>`.

- Idioma: sujeto/cuerpo/footer en español; tipo siempre en inglés (estándar commitlint).
- Sujeto: imperativo presente, minúsculas, sin punto final, ≤72 chars (ideal ≤50). Detalle en el cuerpo.
- Alcance opcional, kebab-case del área tocada: `eureka`, `docker`, `ci`, `deps` — omitir si es transversal.
- Cuerpo: tras línea en blanco, qué y por qué, no cómo.
- Footer: tras línea en blanco; `Closes #N`/`Fixes #N`; breaking con `BREAKING CHANGE:` o sufijo `!`.
- Nunca agregar `Co-Authored-By`, firma de agente/IA, ni enlaces de sesión a un commit o PR, salvo pedido explícito del usuario para ese commit puntual.

### 11.0 Reglas de la spec (MUST)

- Header: tipo + alcance opcional + `:` + espacio + sujeto.
- `feat` para funcionalidad nueva, `fix` para corrección de bug.
- Cuerpo: qué y por qué, nunca cómo.
- Footer: `Closes #N` / `Fixes #N` para issues.
- Breaking change: footer `BREAKING CHANGE:` o `!` antes de `:`.

### 11.1 Reglas del proyecto

- Sujeto/cuerpo en español, tipo en inglés.
- Sujeto: imperativo presente, minúsculas, sin punto, ≤72 chars.
- Alcance: lista cerrada — `eureka`, `docker`, `ci`, `deps`.
- Enforcement: sin commitlint instalado — el agente valida manualmente.

### 11.2 Gitmoji (adoptado)

Formato: `:emoji: <tipo>(<alcance>)?: <sujeto>`

**Prioridad de selección de emoji (menor a mayor):**

1. **Por defecto según tipo** (gitmoji.dev):

| Tipo | Emoji |
| --- | --- |
| `feat` | ✨ |
| `fix` | 🐛 |
| `docs` | 📝 |
| `style` | 🎨 |
| `refactor` | ♻️ |
| `perf` | ⚡️ |
| `test` | ✅ |
| `build` | 📦️ |
| `ci` | 👷 |
| `chore` | 🔧 |
| `revert` | ⏪️ |

2. **Específico del catálogo** si encaja mejor: 💥 breaking, 🎉 inicio proyecto, 🔥 quitar código, 💫 animaciones/transiciones, 💄 UI, 🔒️ seguridad, 🚀 deploy, ⬆️/⬇️ dependencias, 🙈 gitignore, 🐋 Docker.
3. **Personalizado libre** si el significado no es ambiguo.

**Versionado semántico:** `feat` → MINOR, `fix` → PATCH. Breaking en cualquier tipo → MAJOR (footer `BREAKING CHANGE:` o `!` antes de `:`).

**Ejemplos:**
```
:sparkles: feat(eureka): habilita registro con lease-renewal configurable
:lock: fix(eureka): agrega autenticación básica al dashboard
:whale: build(docker): agrega healthcheck al contenedor de discovery-server
```

### 11.3 Ramas (Git Flow completo — modelo Driessen)

```
main    ●─────●───────────●───────●──────────●───►
         ▲(tag v1.0) ▲(tag v1.0.1)      ▲(tag v1.1.0)
         │  merge    │ merge             │  merge
  release/1.0.0          │        release/1.1.0
       ▲                 │             ▲
       │  merge          │hotfix/1.0.1 │  merge
develop ●──●───●───●──────●─────●───────●───●───►
          \   \   \            \       /
      feature/a  feature/b   feature/c
```

**Ramas permanentes:** `main` (producción, siempre tagueada), `develop` (integración).

**Ramas de soporte:**

| Tipo | Nace de | Mergea a | Naming |
| --- | --- | --- | --- |
| `feature/*` | `develop` | `develop` | `feature/descripcion-corta` |
| `release/*` | `develop` | `main` + `develop` | `release/x.y.z` |
| `hotfix/*` | `main` | `main` + `develop` | `hotfix/descripcion-corta` |
| `support/*` | tag vieja | solo a sí misma | `support/1.x` |

`--no-ff` siempre. Sin force-push a `main`/`develop`. Sin commit directo a `main`/`develop`.

`(sin repositorio git propio inicializado todavía en discovery-server/ — este esquema es el que se adopta cuando se inicialice, no describe un estado actual con ramas reales)`.

## 12. Límites del agente

**Siempre** (sin pedir permiso): editar código, tests, docs dentro del repo; crear commits locales.

**Preguntar primero**: force-push, `git reset --hard`, agregar/actualizar dependencias, deploy a staging.

**Nunca sin aprobación explícita**:
- Configuración de CI/CD (`.github/workflows/docker-publish.yml`).
- Archivos de secretos o `.env`.
- Deploy a producción.

## 13. Deploy

```bash
# CI ya existente: .github/workflows/docker-publish.yml
# se dispara en push a main, construye y publica a Docker Hub
# tag: ${DOCKER_USERNAME}/discovery-server:latest

# build manual local equivalente
docker build -t discovery-server .
docker run -p 8761:8761 discovery-server
```

## 14. Monorepo

Este microservicio es parte del workspace Convivo. El `AGENTS.md` de la raíz del workspace (`cloud-native/AGENTS.md`) define reglas transversales (infraestructura, Trello, etc.). Este archivo gana sobre ese para código dentro de `discovery-server/`.

## 15. Enforcement

Orientativo, no forzado mecánicamente. Las reglas críticas (secretos, auth del dashboard Eureka) deben reforzarse con CI, no depender solo de este texto.

| Regla | Hook local | CI | Solo texto |
| --- | --- | --- | --- |
| Secretos (§10) | — | sin escaneo configurado hoy | — |
| Build/tests (§4, §7) | — | sin CI de build/test hoy (solo publish) | — |
| Commits (§11) | — | — | validación manual |

## 16. Mantenimiento

Tratar como código. Revisar cuando se agregue autenticación al dashboard de Eureka (§10).

## 17. Normativa y cumplimiento

Normas que aplican: ISO/IEC 27001 (controles técnicos, ver hallazgo de §10).

### 17.1 ISO/IEC 25010

`(no aplica: microservicio de infraestructura sin interfaz propia — los atributos de interacción y usabilidad no aplican)`

### 17.2 ISO/IEC 27001 — SGSI

| Propiedad | Control mínimo | Evidencia |
| --- | --- | --- |
| Confidencialidad | **roto, no solo ausente** — `spring.security.user` con credencial hardcodeada en `discovery-server.yml`, pero sin `spring-boot-starter-security` en el classpath (ver §10); sin TLS en el dashboard/API | `(no aplica: brecha conocida, sin evidencia de control funcional)` |
| Integridad | registro de instancias gestionado por Eureka en memoria, sin persistencia externa editable | comportamiento estándar de Eureka Server |
| Disponibilidad | healthcheck configurado en `docker-compose.yml` (`/actuator/health`, 5 reintentos) | `docker-compose.yml` |

Controles del Anexo A aplicables:

| Control | Dónde vive |
| --- | --- |
| A.8.3 Restricción de acceso | **no implementado efectivamente** — configurado en YAML pero inerte sin el starter de seguridad (§10) |
| A.8.5 Autenticación segura | credencial `admin/admin123` hardcodeada en el repo — no cumple, aunque además esté inerte |
| A.8.9 Gestión de configuración | `application.yml` sin defaults inseguros más allá de la falta de auth ya señalada |
| A.8.24 Uso de criptografía | `(no aplica: sin criptografía propia)` |

### 17.3 ISO 9001 / IEEE 730 / ISO/IEC/IEEE 29119

`(no aplica: sin proceso formal de pruebas documentado)`

### 17.4 Cruce con normativa chilena

`(no aplica: discovery-server no trata datos personales — solo metadatos de infraestructura de los servicios registrados)`

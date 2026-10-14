# discovery-server-cloud

Servidor de descubrimiento y registro de microservicios Spring Cloud Netflix Eureka para la plataforma Convivo.

## Descripción

`discovery-server-cloud` es un servicio basado en Java 25 y Spring Boot 4.1.1 con Spring Cloud Netflix Eureka Server. Proporciona localización dinámica de instancias, balanceo de carga en capa de aplicación y monitoreo de salud para los microservicios del ecosistema.

## Verificación Preventiva Local Pre-push

Antes de abrir un Pull Request o enviar commits al repositorio remoto, ejecutar las validaciones con el runner nativo de Maven:

```bash
# 1. Ejecutar pruebas unitarias y validación Gitmoji
./mvnw clean test

# 2. Ejecutar validación completa de empaquetado, enforcer y cobertura JaCoCo
./mvnw verify
```

> **Nota de CI**: `src/test/java/com/convivo/discovery_server/GitCommitConventionTest.java` asegura que todos los mensajes de commit cumplan la convención Gitmoji + Conventional Commits requerida por `pr-quality.yml`.

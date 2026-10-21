package com.convivo.discovery_server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validador de convención Gitmoji + Conventional Commits para discovery-server-cloud.
 * Alineado con pr-quality.yml y AGENTS.md §11.2.
 */
class GitCommitConventionTest {

    private static final Pattern GITMOJI_CONVENTIONAL_PATTERN =
        Pattern.compile("^(?::\\w+:|[^\\w\\s:]\\S*)\\s(\\w*)(?:\\(([\\w$.\\-*/ ]*)\\))?!?:\\s(.*)$");

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "feat",
        "fix",
        "docs",
        "style",
        "refactor",
        "perf",
        "test",
        "build",
        "ci",
        "chore",
        "revert"
    );

    private boolean isValidCommitMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String firstLine = message.trim().lines().findFirst().orElse("").trim();
        Matcher matcher = GITMOJI_CONVENTIONAL_PATTERN.matcher(firstLine);
        if (!matcher.matches()) {
            return false;
        }
        String type = matcher.group(1);
        return ALLOWED_TYPES.contains(type);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ":sparkles: feat: registro de instancias en cluster eureka",
        "✨ feat(registry): soporte para heartbeat y auto-preservation",
        ":lock: fix(security): asegurar endpoints con autenticacion basica",
        ":memo: docs(readme): documentar verificacion local pre-push",
        ":white_check_mark: test(registry): cubrir lease renewal de clientes",
        "🐛 fix(eureka): corregir expiracion de registros huerfanos",
        ":wrench: chore(deps): actualizar librerias menores"
    })
    @DisplayName("Debe aceptar mensajes con Gitmoji y Conventional Commits válidos")
    void debeAceptarMensajesValidos(String commitMessage) {
        assertTrue(isValidCommitMessage(commitMessage));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "fix: sin gitmoji al inicio",
        ":sparkles: sin tipo convencional",
        "WIP prueba rapida",
        ":sparkles: invalid_type(modulo): descripcion",
        "feat: solo texto",
        ""
    })
    @DisplayName("Debe rechazar mensajes sin Gitmoji o formato inválido")
    void debeRechazarMensajesInvalidos(String commitMessage) {
        assertFalse(isValidCommitMessage(commitMessage));
    }
}

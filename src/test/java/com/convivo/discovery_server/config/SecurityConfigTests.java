package com.convivo.discovery_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Verifica las reglas de acceso de {@link SecurityConfig} contra el servidor real.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.ssl.enabled=false")
class SecurityConfigTests {

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Test
    void healthEsPublico() throws Exception {
        assertThat(get("/actuator/health").statusCode()).isEqualTo(200);
    }

    @Test
    void apiEurekaSinCredencialesDevuelve401() throws Exception {
        assertThat(get("/eureka/apps").statusCode()).isEqualTo(401);
    }

    @Test
    void dashboardSinCredencialesDevuelve401() throws Exception {
        assertThat(get("/").statusCode()).isEqualTo(401);
    }

    private HttpResponse<Void> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        return client.send(request, HttpResponse.BodyHandlers.discarding());
    }
}

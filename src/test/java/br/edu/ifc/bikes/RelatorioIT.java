package br.edu.ifc.bikes;

import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.jwt.JwtUtils;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Confirma que o endpoint protegido retorna um PDF real gerado pelo JasperReports. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/usuarios/usuarios-insert.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/usuarios/usuarios-delete.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RelatorioIT {
    @LocalServerPort
    int port;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    WebTestClient testClient;
    String adminAuthorization;
    String clienteAuthorization;

    @BeforeEach
    void setUp() {
        for (Long id : List.of(100L, 101L, 102L)) {
            Usuario usuario = usuarioRepository.findById(id).orElseThrow();
            usuario.setPassword(passwordEncoder.encode("123456"));
            usuarioRepository.save(usuario);
        }
        testClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
        adminAuthorization = token("jose@gmail.com", "ADMIN");
        clienteAuthorization = token("maria@gmail.com", "CLIENTE");
    }

    @Test
    void report_adminReceivesPdfContent() {
        byte[] response = testClient.get().uri("/api/v1/relatorios/locacoes")
                .header("Authorization", adminAuthorization)
                .exchange().expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .expectBody().returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(new String(response, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void report_clientIsForbidden() {
        testClient.get().uri("/api/v1/relatorios/locacoes")
                .header("Authorization", clienteAuthorization)
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void report_withoutAuthenticationIsUnauthorized() {
        testClient.get().uri("/api/v1/relatorios/locacoes")
                .exchange().expectStatus().isUnauthorized();
    }

    private String token(String username, String role) {
        return "Bearer " + JwtUtils.createToken(username, role).getToken();
    }
}

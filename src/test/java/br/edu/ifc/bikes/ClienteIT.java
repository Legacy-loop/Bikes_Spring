package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.ClienteResponseDTO;
import br.edu.ifc.bikes.entity.Usuario;
import br.edu.ifc.bikes.jwt.JwtUtils;
import br.edu.ifc.bikes.repository.UsuarioRepository;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/usuarios/usuarios-insert.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/usuarios/usuarios-delete.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ClienteIT {
    @LocalServerPort
    int port;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    WebTestClient testClient;
    String clienteAuthorization;
    String outroClienteAuthorization;
    String adminAuthorization;

    @BeforeEach
    void setUp() {
        for (Long id : List.of(100L, 101L, 102L)) {
            Usuario usuario = usuarioRepository.findById(id).orElseThrow();
            usuario.setPassword(passwordEncoder.encode("123456"));
            usuarioRepository.save(usuario);
        }

        testClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        clienteAuthorization = token("maria@gmail.com", "CLIENTE");
        outroClienteAuthorization = token("joao@gmail.com", "CLIENTE");
        adminAuthorization = token("jose@gmail.com", "ADMIN");
    }

    @Test
    void create_withoutAuthentication_returnsUnauthorized() {
        testClient.post()
                .uri("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(client("Maria", "12345678901"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void create_validDataAssociatesAuthenticatedUserAndSetsAuditFields() {
        ClienteResponseDTO response = testClient.post()
                .uri("/api/v1/clientes")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(client("Maria da Silva", "12345678901"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ClienteResponseDTO.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.nome()).isEqualTo("Maria da Silva");
        assertThat(response.criadoPor()).isEqualTo("maria@gmail.com");
        assertThat(response.dataCriacao()).isNotNull();

        ClienteResponseDTO own = testClient.get()
                .uri("/api/v1/clientes/me")
                .header("Authorization", clienteAuthorization)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ClienteResponseDTO.class)
                .returnResult().getResponseBody();
        assertThat(own).isNotNull();
        assertThat(own.id()).isEqualTo(response.id());

        testClient.get()
                .uri("/api/v1/clientes/me")
                .header("Authorization", outroClienteAuthorization)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void create_duplicateCpf_returnsConflict() {
        createClient("12345678901");
        ErrorMessage response = testClient.post()
                .uri("/api/v1/clientes")
                .header("Authorization", outroClienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(client("Joao", "12345678901"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ErrorMessage.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void create_invalidFields_returnsUnprocessableContent() {
        testClient.post()
                .uri("/api/v1/clientes")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ClienteRequestDTO("", "123", "", "email-invalido"))
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void adminCannotCreateClient() {
        testClient.post()
                .uri("/api/v1/clientes")
                .header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(client("Admin", "12345678901"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void regularClientCannotListAllAndAdminCan() {
        createClient("12345678901");

        testClient.get()
                .uri("/api/v1/clientes")
                .header("Authorization", clienteAuthorization)
                .exchange()
                .expectStatus().isForbidden();

        ClienteResponseDTO[] response = testClient.get()
                .uri("/api/v1/clientes")
                .header("Authorization", adminAuthorization)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ClienteResponseDTO[].class)
                .returnResult().getResponseBody();

        assertThat(response).hasSize(1);
    }

    private void createClient(String cpf) {
        testClient.post()
                .uri("/api/v1/clientes")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(client("Maria", cpf))
                .exchange()
                .expectStatus().isCreated();
    }

    private ClienteRequestDTO client(String nome, String cpf) {
        return new ClienteRequestDTO(nome, cpf, "47999999999", nome.toLowerCase().replace(" ", "") + "@example.com");
    }

    private String token(String username, String role) {
        return "Bearer " + JwtUtils.createToken(username, role).getToken();
    }
}

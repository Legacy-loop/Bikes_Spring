package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
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
class BicicletaIT {
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
        for (Long id : List.of(100L, 101L)) {
            Usuario usuario = usuarioRepository.findById(id).orElseThrow();
            usuario.setPassword(passwordEncoder.encode("123456"));
            usuarioRepository.save(usuario);
        }

        testClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        adminAuthorization = token("jose@gmail.com", "ADMIN");
        clienteAuthorization = token("maria@gmail.com", "CLIENTE");
    }

    @Test
    void create_withoutAuthentication_returnsUnauthorized() {
        testClient.post()
                .uri("/api/v1/bicicletas")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bicycle("BIKE-01"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void bicycleActions_areRestrictedToAdmin() {
        testClient.post()
                .uri("/api/v1/bicicletas")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bicycle("BIKE-01"))
                .exchange()
                .expectStatus().isForbidden();

        testClient.get()
                .uri("/api/v1/bicicletas")
                .header("Authorization", clienteAuthorization)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void adminCanCreateAndFindByUniqueCode() {
        BicicletaResponseDTO created = testClient.post()
                .uri("/api/v1/bicicletas")
                .header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bicycle("BIKE-01"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BicicletaResponseDTO.class)
                .returnResult().getResponseBody();

        assertThat(created).isNotNull();
        assertThat(created.id().toString()).isNotEqualTo(created.codigo());
        assertThat(created.status()).isEqualTo(Bicicleta.Status.DISPONIVEL);
        assertThat(created.criadoPor()).isEqualTo("jose@gmail.com");

        BicicletaResponseDTO found = testClient.get()
                .uri("/api/v1/bicicletas/codigo/BIKE-01")
                .header("Authorization", adminAuthorization)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BicicletaResponseDTO.class)
                .returnResult().getResponseBody();

        assertThat(found).isNotNull();
        assertThat(found.id()).isEqualTo(created.id());

        BicicletaResponseDTO[] bikes = testClient.get()
                .uri("/api/v1/bicicletas")
                .header("Authorization", adminAuthorization)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BicicletaResponseDTO[].class)
                .returnResult().getResponseBody();
        assertThat(bikes).hasSize(1);
    }

    @Test
    void duplicateCode_returnsConflict() {
        createBicycle("BIKE-01");
        ErrorMessage response = testClient.post()
                .uri("/api/v1/bicicletas")
                .header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bicycle("BIKE-01"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ErrorMessage.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(409);
    }

    @Test
    void invalidInput_returnsUnprocessableContent() {
        testClient.post()
                .uri("/api/v1/bicicletas")
                .header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new BicicletaRequestDTO("", "", null))
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    private void createBicycle(String codigo) {
        testClient.post()
                .uri("/api/v1/bicicletas")
                .header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(bicycle(codigo))
                .exchange()
                .expectStatus().isCreated();
    }

    private BicicletaRequestDTO bicycle(String codigo) {
        return new BicicletaRequestDTO(codigo, "Mountain bike", Bicicleta.Status.DISPONIVEL);
    }

    private String token(String username, String role) {
        return "Bearer " + JwtUtils.createToken(username, role).getToken();
    }
}

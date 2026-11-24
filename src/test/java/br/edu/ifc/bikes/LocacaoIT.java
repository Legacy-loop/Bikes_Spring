package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Locacao;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Integra o fluxo de solicitação de locação com autenticação, estoque e validações. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/usuarios/usuarios-insert.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/usuarios/usuarios-delete.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class LocacaoIT {
    @LocalServerPort
    int port;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    WebTestClient testClient;
    String adminAuthorization;
    String clienteAuthorization;
    String outroClienteAuthorization;

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
        outroClienteAuthorization = token("joao@gmail.com", "CLIENTE");
    }

    @Test
    void create_withoutAuthentication_returnsUnauthorized() {
        testClient.post().uri("/api/v1/locacoes")
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void adminCannotRequestRental() {
        testClient.post().uri("/api/v1/locacoes")
                .header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request())
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void clientWithoutCustomerRegistration_returnsNotFound() {
        testClient.post().uri("/api/v1/locacoes")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void request_withoutAvailableBicycle_returnsConflict() {
        createClient(clienteAuthorization, "Maria", "12345678901");
        testClient.post().uri("/api/v1/locacoes")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request())
                .exchange().expectStatus().isEqualTo(409);
    }

    @Test
    void create_validRequestChoosesBikeAndMarksItAsRented() {
        createClient(clienteAuthorization, "Maria", "12345678901");
        createBicycle("BIKE-01");

        LocacaoResponseDTO response = createRental(clienteAuthorization);

        assertThat(response.id()).isNotNull();
        assertThat(response.nomeCliente()).isEqualTo("Maria");
        assertThat(response.codigoBicicleta()).isEqualTo("BIKE-01");
        assertThat(response.status()).isEqualTo(Locacao.Status.ATIVA);
        assertThat(response.dataCriacao()).isNotNull();
        assertThat(response.criadoPor()).isEqualTo("maria@gmail.com");
        assertThat(response.duracaoMinutos()).isNull();
        assertThat(response.valor()).isNull();

        BicicletaResponseDTO bicycle = findBicycle("BIKE-01");
        assertThat(bicycle.status()).isEqualTo(Bicicleta.Status.ALUGADA);
    }

    @Test
    void create_withOnlyOneAvailableBicycle_secondRequestReturnsConflict() {
        createClient(clienteAuthorization, "Maria", "12345678901");
        createClient(outroClienteAuthorization, "Joao", "12345678902");
        createBicycle("BIKE-01");
        createRental(clienteAuthorization);

        testClient.post().uri("/api/v1/locacoes")
                .header("Authorization", outroClienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request())
                .exchange().expectStatus().isEqualTo(409);
    }

    @Test
    void create_invalidCardOrPastReturnDate_returnsUnprocessableContent() {
        createClient(clienteAuthorization, "Maria", "12345678901");
        createBicycle("BIKE-01");

        testClient.post().uri("/api/v1/locacoes")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoRequestDTO(LocalDateTime.now().plusDays(1), "123", "Maria", "12/29", "123"))
                .exchange().expectStatus().isEqualTo(422);

        testClient.post().uri("/api/v1/locacoes")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoRequestDTO(LocalDateTime.now().minusDays(1), "4111111111111111", "Maria", "12/29", "123"))
                .exchange().expectStatus().isEqualTo(422);
    }

    @Test
    void list_isRestrictedToAdmin() {
        createClient(clienteAuthorization, "Maria", "12345678901");
        createBicycle("BIKE-01");
        createRental(clienteAuthorization);

        testClient.get().uri("/api/v1/locacoes")
                .header("Authorization", clienteAuthorization)
                .exchange().expectStatus().isForbidden();

        LocacaoResponseDTO[] response = testClient.get().uri("/api/v1/locacoes")
                .header("Authorization", adminAuthorization)
                .exchange().expectStatus().isOk()
                .expectBody(LocacaoResponseDTO[].class).returnResult().getResponseBody();
        assertThat(response).hasSize(1);
    }

    private void createClient(String authorization, String nome, String cpf) {
        testClient.post().uri("/api/v1/clientes").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ClienteRequestDTO(nome, cpf, "47999999999", nome.toLowerCase() + "@example.com"))
                .exchange().expectStatus().isCreated();
    }

    private void createBicycle(String codigo) {
        testClient.post().uri("/api/v1/bicicletas").header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new BicicletaRequestDTO(codigo, "Mountain bike", Bicicleta.Status.DISPONIVEL))
                .exchange().expectStatus().isCreated();
    }

    private LocacaoResponseDTO createRental(String authorization) {
        return testClient.post().uri("/api/v1/locacoes").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(request())
                .exchange().expectStatus().isCreated()
                .expectBody(LocacaoResponseDTO.class).returnResult().getResponseBody();
    }

    private BicicletaResponseDTO findBicycle(String codigo) {
        return testClient.get().uri("/api/v1/bicicletas/codigo/" + codigo)
                .header("Authorization", adminAuthorization)
                .exchange().expectStatus().isOk()
                .expectBody(BicicletaResponseDTO.class).returnResult().getResponseBody();
    }

    private LocacaoRequestDTO request() {
        return new LocacaoRequestDTO(LocalDateTime.now().plusDays(1), "4111111111111111", "MARIA DA SILVA", "12/29", "123");
    }

    private String token(String username, String role) {
        return "Bearer " + JwtUtils.createToken(username, role).getToken();
    }
}

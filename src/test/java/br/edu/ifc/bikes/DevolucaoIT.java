package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.ClienteRequestDTO;
import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Valida o registro administrativo de devoluções e os efeitos no valor e na bicicleta. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/usuarios/usuarios-insert.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/usuarios/usuarios-delete.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DevolucaoIT {
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
    void return_successFinalizesRentalChargesMinimumAndReleasesBike() {
        Long rentalId = createRental();

        LocacaoResponseDTO response = returnRental(rentalId, false, adminAuthorization, 200);

        assertThat(response.status()).isEqualTo(Locacao.Status.FINALIZADA);
        assertThat(response.dataDevolucao()).isNotNull();
        assertThat(response.dataModificacao()).isNotNull();
        assertThat(response.modificadoPor()).isEqualTo("jose@gmail.com");
        assertThat(response.duracaoMinutos()).isNotNull();
        assertThat(response.valor()).isGreaterThanOrEqualTo(new BigDecimal("10.00"));
        assertThat(findBicycle().status()).isEqualTo(Bicicleta.Status.DISPONIVEL);
    }

    @Test
    void return_withProblemMarksBicycleForMaintenance() {
        Long rentalId = createRental();

        returnRental(rentalId, true, adminAuthorization, 200);

        assertThat(findBicycle().status()).isEqualTo(Bicicleta.Status.MANUTENCAO);
    }

    @Test
    void return_twiceReturnsConflict() {
        Long rentalId = createRental();
        returnRental(rentalId, false, adminAuthorization, 200);

        testClient.post().uri("/api/v1/locacoes/{id}/devolucao", rentalId)
                .header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(new DevolucaoRequestDTO(false))
                .exchange().expectStatus().isEqualTo(409);
    }

    @Test
    void return_nonexistentRentalReturnsNotFound() {
        testClient.post().uri("/api/v1/locacoes/99999/devolucao")
                .header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(new DevolucaoRequestDTO(false))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void clientCannotRegisterReturn() {
        Long rentalId = createRental();
        testClient.post().uri("/api/v1/locacoes/{id}/devolucao", rentalId)
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(new DevolucaoRequestDTO(false))
                .exchange().expectStatus().isForbidden();
    }

    private Long createRental() {
        testClient.post().uri("/api/v1/clientes").header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ClienteRequestDTO("Maria", "12345678901", "47999999999", "maria@example.com"))
                .exchange().expectStatus().isCreated();
        testClient.post().uri("/api/v1/bicicletas").header("Authorization", adminAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new BicicletaRequestDTO("BIKE-01", "Mountain bike", Bicicleta.Status.DISPONIVEL))
                .exchange().expectStatus().isCreated();
        LocacaoResponseDTO rental = testClient.post().uri("/api/v1/locacoes")
                .header("Authorization", clienteAuthorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LocacaoRequestDTO(LocalDateTime.now().plusDays(1), "4111111111111111", "MARIA DA SILVA", "12/29", "123"))
                .exchange().expectStatus().isCreated()
                .expectBody(LocacaoResponseDTO.class).returnResult().getResponseBody();
        return rental.id();
    }

    private LocacaoResponseDTO returnRental(Long id, boolean problema, String authorization, int status) {
        return testClient.post().uri("/api/v1/locacoes/{id}/devolucao", id)
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).bodyValue(new DevolucaoRequestDTO(problema))
                .exchange().expectStatus().isEqualTo(status)
                .expectBody(LocacaoResponseDTO.class).returnResult().getResponseBody();
    }

    private BicicletaResponseDTO findBicycle() {
        return testClient.get().uri("/api/v1/bicicletas/codigo/BIKE-01")
                .header("Authorization", adminAuthorization)
                .exchange().expectStatus().isOk()
                .expectBody(BicicletaResponseDTO.class).returnResult().getResponseBody();
    }

    private String token(String username, String role) {
        return "Bearer " + JwtUtils.createToken(username, role).getToken();
    }
}

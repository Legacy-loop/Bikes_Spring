package br.edu.ifc.bikes;

import br.edu.ifc.bikes.dto.UsuarioSenhaDto;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/usuarios/usuarios-insert.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/usuarios/usuarios-delete.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UsuarioSenhaIT {
    @LocalServerPort
    int port;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    WebTestClient testClient;
    String authorization;

    @BeforeEach
    void setUp() {
        Usuario usuario = usuarioRepository.findById(101L).orElseThrow();
        usuario.setPassword(passwordEncoder.encode("123456"));
        usuarioRepository.save(usuario);

        testClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        authorization = "Bearer " + JwtUtils.createToken("maria@gmail.com", "CLIENTE").getToken();
    }

    @Test
    void updatePassword_validData_returnsNoContent() {
        testClient.patch()
                .uri("/api/v1/usuarios/101")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UsuarioSenhaDto("abcdef", "abcdef", "123456"))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    @Test
    void updatePassword_invalidCurrentPassword_returnsBadRequest() {
        testClient.patch()
                .uri("/api/v1/usuarios/101")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UsuarioSenhaDto("abcdef", "abcdef", "654321"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updatePassword_passwordsDoNotMatch_returnsBadRequest() {
        testClient.patch()
                .uri("/api/v1/usuarios/101")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UsuarioSenhaDto("abcdef", "abcdeg", "123456"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updatePassword_blankShortAndLongFields_returnsUnprocessableContent() {
        assertInvalidPassword("", "abcdef", "123456");
        assertInvalidPassword("12345", "abcdef", "123456");
        assertInvalidPassword("1234567", "abcdef", "123456");
        assertInvalidPassword("abcdef", "", "123456");
        assertInvalidPassword("abcdef", "12345", "123456");
        assertInvalidPassword("abcdef", "1234567", "123456");
        assertInvalidPassword("abcdef", "abcdef", "");
        assertInvalidPassword("abcdef", "abcdef", "12345");
        assertInvalidPassword("abcdef", "abcdef", "1234567");
    }

    private void assertInvalidPassword(String novaSenha, String confirmaSenha, String senhaAtual) {
        testClient.patch()
                .uri("/api/v1/usuarios/101")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UsuarioSenhaDto(novaSenha, confirmaSenha, senhaAtual))
                .exchange()
                .expectStatus().isEqualTo(422);
    }
}

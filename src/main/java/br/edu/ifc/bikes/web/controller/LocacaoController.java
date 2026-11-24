package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.jwt.JwtUserDetails;
import br.edu.ifc.bikes.service.LocacaoService;
import br.edu.ifc.bikes.web.exception.ErrorMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Expõe a solicitação de locações, sua consulta administrativa e a devolução no balcão. */
@Tag(name = "Locações", description = "Solicitação, consulta administrativa e devolução de bicicletas.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/locacoes")
@RequiredArgsConstructor
public class LocacaoController {
    private final LocacaoService locacaoService;

    @Operation(summary = "Solicitar locação", description = "O sistema escolhe uma bicicleta disponível e valida os dados do cartão sem armazená-los; o pagamento é simulado.", responses = {
            @ApiResponse(responseCode = "201", description = "Locação criada",
                    content = @Content(schema = @Schema(implementation = LocacaoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Apenas CLIENTE pode solicitar"),
            @ApiResponse(responseCode = "404", description = "Usuário sem cadastro de cliente",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "409", description = "Não há bicicletas disponíveis",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<LocacaoResponseDTO> create(
            @Valid @RequestBody LocacaoRequestDTO request,
            @AuthenticationPrincipal JwtUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(locacaoService.create(request, principal.getId()));
    }

    @Operation(summary = "Listar locações", description = "Lista todas as locações com cliente e bicicleta; operação exclusiva de ADMIN.", responses = {
            @ApiResponse(responseCode = "200", description = "Locações cadastradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LocacaoResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Apenas ADMIN pode listar")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LocacaoResponseDTO>> getAll() {
        return ResponseEntity.ok(locacaoService.getAll());
    }

    @Operation(summary = "Registrar devolução", description = "Finaliza a locação, calcula o valor por horas iniciadas e define o status da bicicleta conforme o estado informado.", responses = {
            @ApiResponse(responseCode = "200", description = "Devolução registrada",
                    content = @Content(schema = @Schema(implementation = LocacaoResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Apenas ADMIN pode registrar devolução"),
            @ApiResponse(responseCode = "404", description = "Locação não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "409", description = "Locação já finalizada",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping("/{id}/devolucao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LocacaoResponseDTO> devolver(
            @PathVariable Long id,
            @Valid @RequestBody DevolucaoRequestDTO request) {
        return ResponseEntity.ok(locacaoService.devolver(id, request));
    }
}

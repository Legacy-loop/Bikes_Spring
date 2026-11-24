package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.dto.BicicletaRequestDTO;
import br.edu.ifc.bikes.dto.BicicletaResponseDTO;
import br.edu.ifc.bikes.dto.BicicletaStatusDTO;
import br.edu.ifc.bikes.service.BicicletaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Bicicletas", description = "Cadastro e consulta de bicicletas.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/bicicletas")
@RequiredArgsConstructor
public class BicicletaController {
    private final BicicletaService bicicletaService;

    @Operation(summary = "Cadastrar bicicleta", responses = {
            @ApiResponse(responseCode = "201", description = "Bicicleta cadastrada",
                    content = @Content(schema = @Schema(implementation = BicicletaResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Código já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PostMapping
    public ResponseEntity<BicicletaResponseDTO> create(@Valid @RequestBody BicicletaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bicicletaService.create(request));
    }

    @Operation(summary = "Localizar bicicleta pelo código")
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<BicicletaResponseDTO> getByCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(bicicletaService.getByCodigo(codigo));
    }

    @Operation(summary = "Listar bicicletas", responses = {
            @ApiResponse(responseCode = "200", description = "Bicicletas cadastradas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BicicletaResponseDTO.class))))
    })
    @GetMapping
    public ResponseEntity<List<BicicletaResponseDTO>> getAll() {
        return ResponseEntity.ok(bicicletaService.getAll());
    }

    @Operation(summary = "Alterar status da bicicleta", description = "Atualiza o status administrativo; uma bicicleta alugada ou o status ALUGADA não podem ser alterados manualmente.", responses = {
            @ApiResponse(responseCode = "200", description = "Status atualizado",
                    content = @Content(schema = @Schema(implementation = BicicletaResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Apenas ADMIN pode alterar o status"),
            @ApiResponse(responseCode = "404", description = "Bicicleta não encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "409", description = "Status incompatível com as regras da locação",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class))),
            @ApiResponse(responseCode = "422", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorMessage.class)))
    })
    @PatchMapping("/codigo/{codigo}/status")
    public ResponseEntity<BicicletaResponseDTO> updateStatus(
            @PathVariable String codigo,
            @Valid @RequestBody BicicletaStatusDTO request) {
        return ResponseEntity.ok(bicicletaService.updateStatus(codigo, request.status()));
    }
}

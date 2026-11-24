package br.edu.ifc.bikes.web.controller;

import br.edu.ifc.bikes.service.RelatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Disponibiliza o relatório Jasper de locações como documento PDF inline. */
@Tag(name = "Relatórios", description = "Relatórios administrativos da API.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
public class RelatorioController {
    private final RelatorioService relatorioService;

    @Operation(summary = "Gerar relatório de locações", description = "Gera com JasperReports um PDF das locações e devoluções.", responses = {
            @ApiResponse(responseCode = "200", description = "Relatório PDF gerado",
                    content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária"),
            @ApiResponse(responseCode = "403", description = "Apenas ADMIN pode gerar o relatório"),
            @ApiResponse(responseCode = "500", description = "Falha ao compilar ou gerar o relatório")
    })
    @GetMapping(value = "/locacoes", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> locacoes() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"relatorio-locacoes.pdf\"")
                .body(relatorioService.gerarRelatorio());
    }
}

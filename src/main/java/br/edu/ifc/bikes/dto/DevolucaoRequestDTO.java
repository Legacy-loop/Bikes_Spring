package br.edu.ifc.bikes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Informa se a bicicleta apresentou algum defeito ao retornar ao balcão. */
public record DevolucaoRequestDTO(
        @Schema(description = "True quando a bicicleta voltou com defeito", example = "false")
        @NotNull(message = "É obrigatório informar se houve problema")
        Boolean problema
) {
}

package br.edu.ifc.bikes.dto;

import br.edu.ifc.bikes.entity.Bicicleta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Recebe o status administrativo solicitado para uma bicicleta. */
public record BicicletaStatusDTO(
        @Schema(description = "Novo status permitido para a bicicleta", example = "MANUTENCAO")
        @NotNull(message = "O status é obrigatório")
        Bicicleta.Status status
) {
}

package br.edu.ifc.bikes.dto;

import br.edu.ifc.bikes.entity.Bicicleta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BicicletaRequestDTO(
        @NotBlank(message = "O código é obrigatório")
        @Size(max = 50, message = "O código deve ter no máximo 50 caracteres")
        String codigo,
        @NotBlank(message = "O modelo é obrigatório")
        @Size(max = 120, message = "O modelo deve ter no máximo 120 caracteres")
        String modelo,
        @NotNull(message = "O status é obrigatório")
        Bicicleta.Status status
) {
}

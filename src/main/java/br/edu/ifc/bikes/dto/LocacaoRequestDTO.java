package br.edu.ifc.bikes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

/** Valida os dados necessários para iniciar uma locação sem guardar os dados do cartão. */
public record LocacaoRequestDTO(
        @Schema(description = "Data e hora limite para devolver a bicicleta", example = "2027-01-15T18:00:00")
        @NotNull(message = "A data prevista para devolução é obrigatória")
        @Future(message = "A data prevista para devolução deve estar no futuro")
        LocalDateTime dataPrevistaDevolucao,

        @Schema(description = "Número de cartão fictício usado apenas na simulação", example = "4111111111111111")
        @NotBlank(message = "O número do cartão é obrigatório")
        @Pattern(regexp = "\\d{16}", message = "O número do cartão deve conter exatamente 16 dígitos")
        String numeroCartao,

        @Schema(description = "Nome impresso no cartão", example = "MARIA DA SILVA")
        @NotBlank(message = "O nome do titular é obrigatório")
        String nomeTitular,

        @Schema(description = "Validade do cartão no formato MM/AA", example = "12/29")
        @NotBlank(message = "A validade do cartão é obrigatória")
        @Pattern(regexp = "(0[1-9]|1[0-2])/[0-9]{2}", message = "A validade deve estar no formato MM/AA")
        String validade,

        @Schema(description = "CVV de três dígitos", example = "123")
        @NotBlank(message = "O CVV é obrigatório")
        @Pattern(regexp = "\\d{3}", message = "O CVV deve conter exatamente 3 dígitos")
        String cvv
) {
}

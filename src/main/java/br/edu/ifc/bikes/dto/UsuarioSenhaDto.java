package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioSenhaDto(
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 6, max = 6, message = "A senha deve ter exatamente 6 caracteres")
        String novaSenha,
        @NotBlank(message = "A confirmação senha é obrigatória")
        @Size(min = 6, max = 6, message = "A confirmação deve ter exatamente 6 caracteres")
        String confirmaSenha,
        @NotBlank(message = "A senha atual é obrigatória")
        @Size(min = 6, max = 6, message = "A senha atual deve ter exatamente 6 caracteres")
        String senhaAtual
) {
}

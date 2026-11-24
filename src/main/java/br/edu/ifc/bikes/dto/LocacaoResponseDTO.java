package br.edu.ifc.bikes.dto;

import br.edu.ifc.bikes.entity.Locacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Apresenta a locação sem expor dados usados na simulação do pagamento. */
public record LocacaoResponseDTO(
        Long id,
        String nomeCliente,
        String codigoBicicleta,
        LocalDateTime dataRetirada,
        LocalDateTime dataPrevistaDevolucao,
        LocalDateTime dataDevolucao,
        Long duracaoMinutos,
        BigDecimal valor,
        Locacao.Status status,
        LocalDateTime dataCriacao,
        LocalDateTime dataModificacao,
        String criadoPor,
        String modificadoPor
) {
}

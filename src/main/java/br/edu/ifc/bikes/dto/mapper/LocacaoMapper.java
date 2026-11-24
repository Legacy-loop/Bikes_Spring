package br.edu.ifc.bikes.dto.mapper;

import br.edu.ifc.bikes.dto.LocacaoRelatorioDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.entity.Locacao;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeFormatter;

/** Converte locações em respostas HTTP e linhas formatadas para o relatório. */
@Component
public class LocacaoMapper {
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public LocacaoResponseDTO toResponse(Locacao locacao) {
        Long duracaoMinutos = locacao.getDataDevolucao() == null
                ? null
                : Duration.between(locacao.getDataRetirada(), locacao.getDataDevolucao()).toMinutes();
        return new LocacaoResponseDTO(
                locacao.getId(),
                locacao.getCliente().getNome(),
                locacao.getBicicleta().getCodigo(),
                locacao.getDataRetirada(),
                locacao.getDataPrevistaDevolucao(),
                locacao.getDataDevolucao(),
                duracaoMinutos,
                locacao.getValor(),
                locacao.getStatus(),
                locacao.getDataCriacao(),
                locacao.getDataModificacao(),
                locacao.getCriadoPor(),
                locacao.getModificadoPor()
        );
    }

    public LocacaoRelatorioDTO toRelatorio(Locacao locacao) {
        String devolucao = locacao.getDataDevolucao() == null
                ? "-"
                : DATA_HORA.format(locacao.getDataDevolucao());
        String duracao = locacao.getStatus() == Locacao.Status.ATIVA
                ? "Em andamento"
                : formatarDuracao(Duration.between(locacao.getDataRetirada(), locacao.getDataDevolucao()).toMinutes());
        return new LocacaoRelatorioDTO(
                locacao.getCliente().getNome(),
                locacao.getBicicleta().getCodigo(),
                DATA_HORA.format(locacao.getDataRetirada()),
                devolucao,
                duracao,
                locacao.getStatus().name()
        );
    }

    private String formatarDuracao(long minutos) {
        return (minutos / 60) + "h " + (minutos % 60) + "min";
    }
}

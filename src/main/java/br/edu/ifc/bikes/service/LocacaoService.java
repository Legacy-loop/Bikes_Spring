package br.edu.ifc.bikes.service;

import br.edu.ifc.bikes.dto.DevolucaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoRequestDTO;
import br.edu.ifc.bikes.dto.LocacaoResponseDTO;
import br.edu.ifc.bikes.dto.mapper.LocacaoMapper;
import br.edu.ifc.bikes.entity.Bicicleta;
import br.edu.ifc.bikes.entity.Cliente;
import br.edu.ifc.bikes.entity.Locacao;
import br.edu.ifc.bikes.exception.EntityNotFoundException;
import br.edu.ifc.bikes.exception.LocacaoConflictException;
import br.edu.ifc.bikes.repository.BicicletaRepository;
import br.edu.ifc.bikes.repository.ClienteRepository;
import br.edu.ifc.bikes.repository.LocacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Aplica as regras de negócio de locação e devolução dentro de transações. */
@Service
@RequiredArgsConstructor
public class LocacaoService {
    private final LocacaoRepository locacaoRepository;
    private final ClienteRepository clienteRepository;
    private final BicicletaRepository bicicletaRepository;
    private final LocacaoMapper locacaoMapper;

    @Value("${bikes.tarifa-hora:10.00}")
    private BigDecimal tarifaHora;

    @Transactional
    public LocacaoResponseDTO create(LocacaoRequestDTO request, Long usuarioId) {
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId).orElseThrow(
                () -> new EntityNotFoundException("O usuário autenticado ainda não possui cadastro de cliente")
        );
        Bicicleta bicicleta = bicicletaRepository.findFirstByStatusOrderByIdAsc(Bicicleta.Status.DISPONIVEL)
                .orElseThrow(() -> new LocacaoConflictException("Não há bicicletas disponíveis"));
        if (locacaoRepository.existsByBicicletaIdAndStatus(bicicleta.getId(), Locacao.Status.ATIVA)) {
            throw new LocacaoConflictException("A bicicleta já possui uma locação ativa");
        }

        // O pagamento é apenas simulado: os dados de cartão são validados na entrada e nunca persistidos.
        Locacao locacao = new Locacao();
        locacao.setCliente(cliente);
        locacao.setBicicleta(bicicleta);
        locacao.setDataRetirada(LocalDateTime.now());
        locacao.setDataPrevistaDevolucao(request.dataPrevistaDevolucao());
        locacao.setStatus(Locacao.Status.ATIVA);
        bicicleta.setStatus(Bicicleta.Status.ALUGADA);
        bicicletaRepository.save(bicicleta);
        return locacaoMapper.toResponse(locacaoRepository.saveAndFlush(locacao));
    }

    @Transactional(readOnly = true)
    public List<LocacaoResponseDTO> getAll() {
        return locacaoRepository.findTodasComClienteEBicicleta().stream()
                .map(locacaoMapper::toResponse)
                .toList();
    }

    @Transactional
    public LocacaoResponseDTO devolver(Long id, DevolucaoRequestDTO request) {
        Locacao locacao = locacaoRepository.findComClienteEBicicletaById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Locação com id=%d não encontrada", id))
        );
        if (locacao.getStatus() == Locacao.Status.FINALIZADA) {
            throw new LocacaoConflictException("Esta locação já foi finalizada");
        }

        LocalDateTime dataDevolucao = LocalDateTime.now();
        long minutos = Duration.between(locacao.getDataRetirada(), dataDevolucao).toMinutes();
        // Qualquer fração de hora inicia uma nova hora, e o balcão cobra ao menos uma hora.
        long horasCobradas = Math.max(1, (minutos + 59) / 60);
        BigDecimal valor = tarifaHora.multiply(BigDecimal.valueOf(horasCobradas))
                .setScale(2, RoundingMode.HALF_UP);

        locacao.setDataDevolucao(dataDevolucao);
        locacao.setValor(valor);
        locacao.setStatus(Locacao.Status.FINALIZADA);
        locacao.getBicicleta().setStatus(request.problema()
                ? Bicicleta.Status.MANUTENCAO
                : Bicicleta.Status.DISPONIVEL);
        return locacaoMapper.toResponse(locacaoRepository.saveAndFlush(locacao));
    }
}

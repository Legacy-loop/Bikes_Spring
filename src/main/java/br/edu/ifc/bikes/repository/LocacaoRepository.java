package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Locacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Busca locações com seus relacionamentos carregados para responder e gerar relatórios. */
public interface LocacaoRepository extends JpaRepository<Locacao, Long> {
    boolean existsByBicicletaIdAndStatus(Long bicicletaId, Locacao.Status status);

    @Query("select l from Locacao l join fetch l.cliente join fetch l.bicicleta order by l.dataRetirada desc")
    List<Locacao> findTodasComClienteEBicicleta();

    @Query("select l from Locacao l join fetch l.cliente join fetch l.bicicleta where l.id = :id")
    Optional<Locacao> findComClienteEBicicletaById(Long id);
}

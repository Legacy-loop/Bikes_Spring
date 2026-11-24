package br.edu.ifc.bikes.repository;

import br.edu.ifc.bikes.entity.Bicicleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface BicicletaRepository extends JpaRepository<Bicicleta, Long> {
    Optional<Bicicleta> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);

    // O bloqueio impede que duas transações escolham simultaneamente a mesma bicicleta.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Bicicleta> findFirstByStatusOrderByIdAsc(Bicicleta.Status status);
}

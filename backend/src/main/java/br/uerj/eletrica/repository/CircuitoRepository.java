package br.uerj.eletrica.repository;

import br.uerj.eletrica.domain.Circuito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CircuitoRepository extends JpaRepository<Circuito, Long> {

    List<Circuito> findByQuadroIdOrderByNumero(Long quadroId);

    Optional<Circuito> findByIdAndQuadroId(Long id, Long quadroId);

    boolean existsByQuadroIdAndNumero(Long quadroId, Integer numero);

    boolean existsByQuadroIdAndNumeroAndIdNot(Long quadroId, Integer numero, Long id);

    long countByQuadroId(Long quadroId);
}

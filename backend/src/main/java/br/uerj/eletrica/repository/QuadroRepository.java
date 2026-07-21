package br.uerj.eletrica.repository;

import br.uerj.eletrica.domain.Quadro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuadroRepository extends JpaRepository<Quadro, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}

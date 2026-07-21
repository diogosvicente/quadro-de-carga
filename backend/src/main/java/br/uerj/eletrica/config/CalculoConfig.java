package br.uerj.eletrica.config;

import br.uerj.eletrica.calc.CalculadoraAlimentador;
import br.uerj.eletrica.calc.CalculadoraCircuito;
import br.uerj.eletrica.calc.tabelas.TabelasNbr5410;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Expõe o motor de cálculo (puro) como beans — o pacote calc/ não conhece Spring. */
@Configuration
public class CalculoConfig {

    @Bean
    public TabelasNbr5410 tabelasNbr5410() {
        return TabelasNbr5410.carregarDoClasspath();
    }

    @Bean
    public CalculadoraCircuito calculadoraCircuito(TabelasNbr5410 tabelas) {
        return new CalculadoraCircuito(tabelas);
    }

    @Bean
    public CalculadoraAlimentador calculadoraAlimentador(TabelasNbr5410 tabelas) {
        return new CalculadoraAlimentador(tabelas);
    }
}

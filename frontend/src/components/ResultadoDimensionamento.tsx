import type { ResultadoCircuito } from '../types/circuito';
import { StatTile } from './StatTile';
import { amp, mm2, num, pct } from '../utils/formato';

interface LinhaProps {
  rotulo: string;
  valor: string;
  forte?: boolean;
}

/** Linha rótulo/valor usada nas seções de detalhe. */
export function Linha({ rotulo, valor, forte = false }: LinhaProps) {
  return (
    <div className={forte ? 'linha linha-forte' : 'linha'}>
      <dt>{rotulo}</dt>
      <dd>{valor}</dd>
    </div>
  );
}

interface ResultadoDimensionamentoProps {
  resultado: ResultadoCircuito;
  titulo?: string;
}

/**
 * Painel completo do resultado de um circuito: destaques, dimensionamento dos
 * condutores e fatores de correção. Usado no detalhe e na prévia do formulário.
 */
export function ResultadoDimensionamento({
  resultado,
  titulo = 'Resultado do Dimensionamento',
}: ResultadoDimensionamentoProps) {
  const r = resultado;
  return (
    <>
      <section className="cartao">
        <h2>{titulo}</h2>
        <div className="destaques">
          <StatTile rotulo="Corrente de projeto" valor={amp(r.correnteProjetoA)} />
          <StatTile rotulo="Corrente corrigida" valor={amp(r.correnteCorrigidaA)} />
          <StatTile rotulo="Disjuntor recomendado" valor={r.disjuntor.rotulo} />
          <StatTile rotulo="Seção do cabo" valor={mm2(r.secaoFinalMm2)} />
        </div>
        <p className="texto-mudo nota-rodape">
          Potência aparente: {num(r.potenciaVA, 0)} VA · Cabo: {r.rotuloCabo}
        </p>
      </section>

      <section className="cartao">
        <h2>Dimensionamento dos Condutores</h2>
        <dl className="linhas">
          <Linha rotulo="Seção por sobrecorrente" valor={mm2(r.secaoSobrecorrenteMm2)} />
          <Linha rotulo="Seção por queda de tensão (calculada)" valor={mm2(r.secaoQuedaCalculadaMm2)} />
          <Linha rotulo="Seção por queda de tensão (comercial)" valor={mm2(r.secaoQuedaMm2)} />
          <Linha rotulo="Seção mínima normativa" valor={mm2(r.secaoMinimaMm2)} />
          <Linha rotulo="Seção final (máximo)" valor={mm2(r.secaoFinalMm2)} forte />
          <Linha rotulo="Condutor neutro" valor={mm2(r.secaoNeutroMm2)} />
          <Linha rotulo="Condutor terra (PE)" valor={mm2(r.secaoTerraMm2)} />
          <Linha rotulo="Rótulo do cabo" valor={r.rotuloCabo} />
          <Linha rotulo="Queda de tensão calculada" valor={pct(r.quedaCalculadaPct)} />
        </dl>
      </section>

      <section className="cartao">
        <h2>Fatores de Correção</h2>
        <dl className="linhas">
          <Linha rotulo="Fator de agrupamento" valor={num(r.fatorAgrupamento, 2)} />
          <Linha rotulo="Fator de temperatura" valor={num(r.fatorTemperatura, 2)} />
          <Linha rotulo="Fator total combinado" valor={num(r.fatorTotal, 2)} />
        </dl>
      </section>
    </>
  );
}

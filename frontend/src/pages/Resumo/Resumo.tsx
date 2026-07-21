import { useCallback, useEffect, useState } from 'react';
import { Link, useOutletContext, useParams } from 'react-router-dom';
import type { ContextoQuadro } from '../../App';
import { buscarResumoQuadro } from '../../api/quadros';
import { obterReferencias } from '../../api/referencias';
import { Alerta } from '../../components/Alerta';
import { Carregando } from '../../components/Carregando';
import { QuadroForm } from '../../components/QuadroForm';
import { StatTile } from '../../components/StatTile';
import type { TipoCircuito } from '../../types/comum';
import type { ResumoQuadroResponse } from '../../types/quadro';
import type { ReferenciasResponse } from '../../types/referencias';
import { amp, kva, kw, mm2, num, pct, watts } from '../../utils/formato';

/** Resumo do quadro: alimentador geral (tiles) + tabela dos circuitos. */
export function Resumo() {
  const { id } = useParams();
  const quadroId = Number(id);
  const { recarregarQuadro } = useOutletContext<ContextoQuadro>();
  const [resumo, setResumo] = useState<ResumoQuadroResponse | null>(null);
  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [editandoQuadro, setEditandoQuadro] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setErro(null);
      const [r, refs] = await Promise.all([buscarResumoQuadro(quadroId), obterReferencias()]);
      setResumo(r);
      setReferencias(refs);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar o resumo.');
    }
  }, [quadroId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const rotuloTipo = (tipo: TipoCircuito): string =>
    referencias?.tiposCircuito.find((t) => t.codigo === tipo)?.rotulo ?? tipo;

  if (resumo === null) {
    return erro ? (
      <Alerta aoTentarNovamente={() => void carregar()}>{erro}</Alerta>
    ) : (
      <Carregando texto="Carregando resumo…" />
    );
  }

  const alimentador = resumo.alimentador;
  const circuitos = [...resumo.circuitos].sort((a, b) => a.numero - b.numero);

  return (
    <>
      <header className="pagina-cabeca">
        <div>
          <h1>Resumo do Dimensionamento</h1>
          <p className="texto-mudo">{resumo.quadro.nome} — NBR 5410</p>
        </div>
        <button
          type="button"
          className="botao-sec"
          onClick={() => setEditandoQuadro((atual) => !atual)}
        >
          {editandoQuadro ? 'Fechar Configurações' : 'Configurações do Alimentador'}
        </button>
      </header>

      {erro ? <Alerta aoTentarNovamente={() => void carregar()}>{erro}</Alerta> : null}

      {editandoQuadro && referencias ? (
        <QuadroForm
          key={`quadro-${resumo.quadro.id}`}
          inicial={resumo.quadro}
          referencias={referencias}
          aoSalvar={() => {
            setEditandoQuadro(false);
            void carregar();
            recarregarQuadro(); // cabeçalho pode exibir o nome antigo após renomear
          }}
          aoCancelar={() => setEditandoQuadro(false)}
        />
      ) : null}

      {alimentador === null ? (
        <div className="vazio">
          <p>O quadro ainda não tem circuitos — cadastre o primeiro para ver o resumo.</p>
          <Link to={`/quadros/${quadroId}/novo`} className="botao">
            Novo Circuito
          </Link>
        </div>
      ) : (
        <>
          <section className="cartao">
            <h2>Alimentador Geral</h2>
            <div className="tiles">
              <StatTile
                rotulo="Corrente total"
                valor={amp(alimentador.correnteTotalA)}
                legenda={`calculada: ${amp(alimentador.correnteCalculadaA)}`}
              />
              <StatTile
                rotulo="Potência demandada"
                valor={kva(alimentador.cargaDemandadaVA)}
                legenda={`instalada: ${kw(alimentador.potenciaTotalW)}`}
              />
              <StatTile
                rotulo="Disjuntor geral"
                valor={alimentador.disjuntorGeral.rotulo}
                legenda={
                  alimentador.maiorDisjuntorCircuitoA !== null
                    ? `maior circuito: ${num(alimentador.maiorDisjuntorCircuitoA, 0)}A`
                    : undefined
                }
              />
              <StatTile
                rotulo="Cabo alimentador"
                valor={mm2(alimentador.secaoAlimentadorMm2)}
                legenda={
                  alimentador.maiorSecaoCircuitoMm2 !== null
                    ? `maior circuito: ${mm2(alimentador.maiorSecaoCircuitoMm2)}`
                    : undefined
                }
              />
              <StatTile
                rotulo="Queda de tensão calculada"
                valor={pct(alimentador.quedaCalculadaPct)}
                legenda={`admissível: ${pct(resumo.quadro.quedaAdmissivelPct)}`}
              />
              <StatTile
                rotulo="Capacidade do quadro"
                valor={kva(alimentador.capacidadeQuadroVA)}
                legenda={`${alimentador.tensaoV}V · ${alimentador.fases}P`}
              />
            </div>
            <p className="texto-mudo nota-rodape">
              Alimentador: {alimentador.rotuloCabo} · Neutro {mm2(alimentador.secaoNeutroMm2)} ·
              Terra {mm2(alimentador.secaoTerraMm2)}
            </p>
          </section>

          <section className="cartao">
            <h2>Circuitos</h2>
            <div className="tabela-wrap">
              <table>
                <caption className="oculto">Circuitos do quadro com resultados</caption>
                <thead>
                  <tr>
                    <th scope="col">Nº</th>
                    <th scope="col">Descrição</th>
                    <th scope="col">Tipo</th>
                    <th scope="col">Tensão</th>
                    <th scope="col" className="celula-num">Potência</th>
                    <th scope="col" className="celula-num">Ip (A)</th>
                    <th scope="col" className="celula-num">Ic (A)</th>
                    <th scope="col">Disjuntor</th>
                    <th scope="col" className="celula-num">Cabo</th>
                    <th scope="col" className="celula-num">N / PE</th>
                  </tr>
                </thead>
                <tbody>
                  {circuitos.map((c) => (
                    <tr key={c.id}>
                      <td>{c.numero}</td>
                      <td>
                        <Link to={`/quadros/${quadroId}/circuitos/${c.id}`}>
                          {c.descricao ?? `Circuito ${c.numero}`}
                        </Link>
                      </td>
                      <td>{rotuloTipo(c.tipo)}</td>
                      <td>
                        {c.tensaoV}V · {c.fases}P
                      </td>
                      <td className="celula-num">{watts(c.potenciaW)}</td>
                      <td className="celula-num">{num(c.resultado.correnteProjetoA, 1)}</td>
                      <td className="celula-num">{num(c.resultado.correnteCorrigidaA, 1)}</td>
                      <td>{c.resultado.disjuntor.rotulo}</td>
                      <td className="celula-num">{mm2(c.resultado.secaoFinalMm2)}</td>
                      <td className="celula-num">
                        {num(c.resultado.secaoNeutroMm2)}/{num(c.resultado.secaoTerraMm2)} mm²
                      </td>
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={10}>
                      {circuitos.length === 1 ? '1 circuito' : `${circuitos.length} circuitos`} ·
                      Total: {kw(resumo.quadro.potenciaTotalW)}
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </section>
        </>
      )}
    </>
  );
}

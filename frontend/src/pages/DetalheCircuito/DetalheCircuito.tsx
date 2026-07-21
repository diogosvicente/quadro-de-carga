import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { buscarCircuito, removerCircuito } from '../../api/circuitos';
import { obterReferencias } from '../../api/referencias';
import { Alerta } from '../../components/Alerta';
import { Carregando } from '../../components/Carregando';
import { Linha, ResultadoDimensionamento } from '../../components/ResultadoDimensionamento';
import type { CircuitoResponse } from '../../types/circuito';
import type { ReferenciasResponse } from '../../types/referencias';
import { num, pct, watts } from '../../utils/formato';

/** Detalhe de um circuito: resultado do dimensionamento + dados de entrada. */
export function DetalheCircuito() {
  const { id, cid } = useParams();
  const quadroId = Number(id);
  const circuitoId = Number(cid);
  const navigate = useNavigate();

  const [circuito, setCircuito] = useState<CircuitoResponse | null>(null);
  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [excluindo, setExcluindo] = useState(false);

  const carregar = useCallback(async () => {
    try {
      setErro(null);
      const [c, refs] = await Promise.all([
        buscarCircuito(quadroId, circuitoId),
        obterReferencias(),
      ]);
      setCircuito(c);
      setReferencias(refs);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar o circuito.');
    }
  }, [quadroId, circuitoId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const excluir = async () => {
    if (!circuito) return;
    if (!window.confirm(`Excluir o circuito ${circuito.numero}?`)) return;
    setExcluindo(true);
    try {
      await removerCircuito(quadroId, circuitoId);
      navigate(`/quadros/${quadroId}/circuitos`);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao excluir o circuito.');
      setExcluindo(false);
    }
  };

  if (circuito === null || referencias === null) {
    return erro ? (
      <Alerta aoTentarNovamente={() => void carregar()}>{erro}</Alerta>
    ) : (
      <Carregando texto="Carregando circuito…" />
    );
  }

  const c = circuito;
  const rotuloTipo =
    referencias.tiposCircuito.find((t) => t.codigo === c.tipo)?.rotulo ?? c.tipo;
  const rotuloFases =
    referencias.fases.find((f) => f.valor === c.fases)?.rotulo ?? `${c.fases} fases`;
  const rotuloIsolante =
    referencias.isolantes.find((i) => i.codigo === c.isolante)?.rotulo ?? c.isolante;
  const metodo = referencias.metodosInstalacao.find((m) => m.codigo === c.metodoInstalacao);
  const rotuloMetodo = metodo ? `${metodo.codigo} — ${metodo.descricao}` : c.metodoInstalacao;
  const forma = referencias.formasAgrupamento.find((f) => f.ref === c.formaAgrupamentoRef);
  const rotuloForma = forma
    ? `${forma.ref} — ${forma.descricao}`
    : String(c.formaAgrupamentoRef);

  return (
    <>
      <header className="pagina-cabeca">
        <div>
          <h1>
            Circuito {c.numero}
            {c.descricao ? ` — ${c.descricao}` : ''}
          </h1>
          <p className="texto-mudo">
            {c.tensaoV}V · {c.fases}P · {num(c.potenciaW, 0)}W · {rotuloTipo}
          </p>
        </div>
        <div className="acoes-linha">
          <button
            type="button"
            className="botao-sec"
            onClick={() => navigate(`/quadros/${quadroId}/circuitos/${circuitoId}/editar`)}
          >
            Editar
          </button>
          <button
            type="button"
            className="botao-perigo"
            onClick={() => void excluir()}
            disabled={excluindo}
          >
            {excluindo ? 'Excluindo…' : 'Excluir'}
          </button>
        </div>
      </header>

      {erro ? <Alerta>{erro}</Alerta> : null}

      <ResultadoDimensionamento resultado={c.resultado} />

      <section className="cartao">
        <h2>Dados de Entrada</h2>
        <dl className="linhas">
          <Linha rotulo="Tipo de circuito" valor={rotuloTipo} />
          <Linha rotulo="Tensão (V)" valor={`${c.tensaoV} V`} />
          <Linha rotulo="Fases" valor={rotuloFases} />
          <Linha rotulo="Potência total (W)" valor={watts(c.potenciaW)} />
          <Linha rotulo="Fator de potência" valor={num(c.fatorPotencia, 2)} />
          <Linha rotulo="Comprimento do fio (m)" valor={`${num(c.comprimentoM)} m`} />
          <Linha rotulo="Circuitos agrupados" valor={num(c.circuitosAgrupados, 0)} />
          <Linha rotulo="Temperatura ambiente (°C)" valor={`${num(c.temperaturaC, 0)} °C`} />
          <Linha rotulo="Método de instalação" valor={rotuloMetodo} />
          <Linha rotulo="Isolante" valor={rotuloIsolante} />
          <Linha rotulo="Forma de agrupamento" valor={rotuloForma} />
          <Linha rotulo="Fator de demanda" valor={num(c.fatorDemanda, 2)} />
          <Linha rotulo="Queda admissível (%)" valor={pct(c.quedaAdmissivelPct)} />
          <Linha rotulo="Linha subterrânea" valor={c.linhaSubterranea ? 'Sim' : 'Não'} />
        </dl>
      </section>
    </>
  );
}

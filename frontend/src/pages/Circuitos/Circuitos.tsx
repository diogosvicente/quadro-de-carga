import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { listarCircuitos } from '../../api/circuitos';
import { obterReferencias } from '../../api/referencias';
import { Alerta } from '../../components/Alerta';
import { Carregando } from '../../components/Carregando';
import type { CircuitoResponse } from '../../types/circuito';
import type { TipoCircuito } from '../../types/comum';
import type { ReferenciasResponse } from '../../types/referencias';
import { mm2, num } from '../../utils/formato';

/** Lista de circuitos de um quadro. */
export function Circuitos() {
  const { id } = useParams();
  const quadroId = Number(id);
  const [circuitos, setCircuitos] = useState<CircuitoResponse[] | null>(null);
  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    try {
      setErro(null);
      const [lista, refs] = await Promise.all([listarCircuitos(quadroId), obterReferencias()]);
      setCircuitos(lista);
      setReferencias(refs);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar os circuitos.');
    }
  }, [quadroId]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const rotuloTipo = (tipo: TipoCircuito): string =>
    referencias?.tiposCircuito.find((t) => t.codigo === tipo)?.rotulo ?? tipo;

  if (circuitos === null) {
    return erro ? (
      <Alerta aoTentarNovamente={() => void carregar()}>{erro}</Alerta>
    ) : (
      <Carregando texto="Carregando circuitos…" />
    );
  }

  const ordenados = [...circuitos].sort((a, b) => a.numero - b.numero);

  return (
    <>
      <header className="pagina-cabeca">
        <div>
          <h1>Circuitos Cadastrados</h1>
          <p className="texto-mudo">
            {circuitos.length === 1
              ? '1 circuito registrado'
              : `${circuitos.length} circuitos registrados`}
          </p>
        </div>
        {circuitos.length > 0 ? (
          <Link to={`/quadros/${quadroId}/novo`} className="botao">
            Novo Circuito
          </Link>
        ) : null}
      </header>
      {erro ? <Alerta aoTentarNovamente={() => void carregar()}>{erro}</Alerta> : null}
      {ordenados.length === 0 ? (
        <div className="vazio">
          <p>Nenhum circuito cadastrado neste quadro ainda.</p>
          <Link to={`/quadros/${quadroId}/novo`} className="botao">
            Cadastrar o primeiro circuito
          </Link>
        </div>
      ) : (
        <div className="lista-cartoes">
          {ordenados.map((c) => (
            <Link
              key={c.id}
              to={`/quadros/${quadroId}/circuitos/${c.id}`}
              className="cartao cartao-circuito"
            >
              <div className="cartao-circuito-info">
                <h2>
                  Circuito {c.numero}
                  {c.descricao ? ` — ${c.descricao}` : ''}
                </h2>
                <p className="cartao-sub">
                  {c.tensaoV}V · {c.fases}P · {num(c.potenciaW, 0)}W · FP{' '}
                  {num(c.fatorPotencia, 2)}
                </p>
                <p className="cartao-meta">{rotuloTipo(c.tipo)}</p>
              </div>
              <div className="cartao-resultado">
                <span className="cartao-resultado-forte">{c.resultado.disjuntor.rotulo}</span>
                <span>{mm2(c.resultado.secaoFinalMm2)}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}

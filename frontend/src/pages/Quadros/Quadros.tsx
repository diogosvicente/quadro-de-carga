import { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { listarQuadros, removerQuadro } from '../../api/quadros';
import { obterReferencias } from '../../api/referencias';
import { Alerta } from '../../components/Alerta';
import { Carregando } from '../../components/Carregando';
import { QuadroForm } from '../../components/QuadroForm';
import type { QuadroResponse } from '../../types/quadro';
import type { ReferenciasResponse } from '../../types/referencias';
import { kw } from '../../utils/formato';

/** Lista de quadros de distribuição + criar/editar/excluir quadro. */
export function Quadros() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [quadros, setQuadros] = useState<QuadroResponse[] | null>(null);
  const [referencias, setReferencias] = useState<ReferenciasResponse | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [formAberto, setFormAberto] = useState<'novo' | number | null>(null);

  const carregar = useCallback(async () => {
    try {
      setErro(null);
      const [listaQuadros, refs] = await Promise.all([listarQuadros(), obterReferencias()]);
      setQuadros(listaQuadros);
      setReferencias(refs);
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao carregar os quadros.');
    }
  }, []);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  const excluir = async (quadro: QuadroResponse) => {
    if (!window.confirm(`Excluir o quadro "${quadro.nome}" e todos os seus circuitos?`)) return;
    try {
      await removerQuadro(quadro.id);
      await carregar();
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Falha ao excluir o quadro.');
    }
  };

  const rotuloSistema = (quadro: QuadroResponse): string =>
    referencias?.sistemasTensao.find((s) => s.codigo === quadro.sistemaTensao)?.rotulo ??
    quadro.sistemaTensao;

  // Usuário típico tem um único quadro: entra direto nele.
  // O link "Quadros" do layout usa /?lista=1 para não redirecionar de volta.
  if (
    quadros !== null &&
    quadros.length === 1 &&
    !params.has('lista') &&
    formAberto === null &&
    erro === null
  ) {
    return <Navigate to={`/quadros/${quadros[0].id}/circuitos`} replace />;
  }

  let corpo: ReactNode;
  if (quadros === null) {
    corpo = erro ? (
      <Alerta aoTentarNovamente={() => void carregar()}>{erro}</Alerta>
    ) : (
      <Carregando texto="Carregando quadros…" />
    );
  } else {
    corpo = (
      <>
        {erro ? <Alerta aoTentarNovamente={() => void carregar()}>{erro}</Alerta> : null}
        {formAberto !== null && referencias ? (
          <QuadroForm
            key={formAberto === 'novo' ? 'novo' : `quadro-${formAberto}`}
            inicial={
              typeof formAberto === 'number'
                ? quadros.find((q) => q.id === formAberto)
                : undefined
            }
            referencias={referencias}
            aoSalvar={(quadro) => {
              if (formAberto === 'novo') {
                navigate(`/quadros/${quadro.id}/circuitos`);
                return;
              }
              setFormAberto(null);
              void carregar();
            }}
            aoCancelar={() => setFormAberto(null)}
          />
        ) : null}
        {quadros.length === 0 ? (
          formAberto === null ? (
            <div className="vazio">
              <p>Nenhum quadro de distribuição cadastrado.</p>
              <button type="button" className="botao" onClick={() => setFormAberto('novo')}>
                Criar o primeiro quadro
              </button>
            </div>
          ) : null
        ) : (
          <div className="grade-quadros">
            {quadros.map((quadro) => (
              <article key={quadro.id} className="cartao cartao-quadro">
                <Link to={`/quadros/${quadro.id}/circuitos`} className="cartao-link">
                  <h2>{quadro.nome}</h2>
                  {quadro.local ? <p className="cartao-sub">{quadro.local}</p> : null}
                  <p className="cartao-meta">
                    {rotuloSistema(quadro)} ·{' '}
                    {quadro.totalCircuitos === 1
                      ? '1 circuito'
                      : `${quadro.totalCircuitos} circuitos`}{' '}
                    · {kw(quadro.potenciaTotalW)}
                  </p>
                </Link>
                <div className="cartao-acoes">
                  <button
                    type="button"
                    className="botao-sec botao-mini"
                    onClick={() => setFormAberto(quadro.id)}
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    className="botao-perigo botao-mini"
                    onClick={() => void excluir(quadro)}
                  >
                    Excluir
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </>
    );
  }

  return (
    <div className="app">
      <header className="topo">
        <div className="topo-inner">
          <div className="topo-marca">
            <span className="topo-quadro">Dimensionamento NBR 5410 — UERJ</span>
          </div>
        </div>
      </header>
      <main className="conteudo conteudo-solto">
        <header className="pagina-cabeca">
          <div>
            <h1>Quadros de Distribuição</h1>
            <p className="texto-mudo">Dimensionamento de circuitos elétricos conforme NBR 5410</p>
          </div>
          {quadros !== null && quadros.length > 0 && formAberto === null ? (
            <button type="button" className="botao" onClick={() => setFormAberto('novo')}>
              Novo Quadro
            </button>
          ) : null}
        </header>
        {corpo}
      </main>
    </div>
  );
}

import { useCallback, useEffect, useState } from 'react';
import {
  BrowserRouter,
  Link,
  Navigate,
  NavLink,
  Outlet,
  Route,
  Routes,
  useParams,
} from 'react-router-dom';
import { buscarQuadro } from './api/quadros';
import type { QuadroResponse } from './types/quadro';
import { Quadros } from './pages/Quadros/Quadros';
import { NovoCircuito } from './pages/NovoCircuito/NovoCircuito';
import { Circuitos } from './pages/Circuitos/Circuitos';
import { DetalheCircuito } from './pages/DetalheCircuito/DetalheCircuito';
import { Resumo } from './pages/Resumo/Resumo';

/**
 * Layout das telas de um quadro: cabeçalho com retorno à lista de quadros e
 * navegação Novo Circuito | Circuitos | Resumo — fixa embaixo no celular,
 * no topo em telas largas (só CSS muda a posição).
 */
export interface ContextoQuadro {
  /** Recarrega o quadro exibido no cabeçalho (ex.: após renomear nas configurações). */
  recarregarQuadro: () => void;
}

function QuadroLayout() {
  const { id } = useParams();
  const quadroId = Number(id);
  const [quadro, setQuadro] = useState<QuadroResponse | null>(null);

  const recarregarQuadro = useCallback(() => {
    buscarQuadro(quadroId)
      .then((q) => setQuadro(q))
      .catch(() => {
        // As páginas internas exibem seus próprios erros; o cabeçalho degrada para o id.
      });
  }, [quadroId]);

  useEffect(() => {
    setQuadro(null);
    recarregarQuadro();
  }, [recarregarQuadro]);

  const classeNav = ({ isActive }: { isActive: boolean }) =>
    isActive ? 'nav-item ativo' : 'nav-item';

  return (
    <div className="app">
      <header className="topo">
        <div className="topo-inner">
          <div className="topo-marca">
            <Link to="/?lista=1" className="topo-voltar">
              Quadros
            </Link>
            <span className="topo-sep" aria-hidden="true">
              /
            </span>
            <span className="topo-quadro">{quadro ? quadro.nome : `Quadro ${quadroId}`}</span>
          </div>
          <nav className="nav-quadro" aria-label="Seções do quadro">
            <NavLink to={`/quadros/${quadroId}/novo`} className={classeNav}>
              Novo Circuito
            </NavLink>
            <NavLink to={`/quadros/${quadroId}/circuitos`} className={classeNav}>
              Circuitos
            </NavLink>
            <NavLink to={`/quadros/${quadroId}/resumo`} className={classeNav}>
              Resumo
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="conteudo">
        <Outlet context={{ recarregarQuadro } satisfies ContextoQuadro} />
      </main>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Quadros />} />
        <Route path="/quadros/:id" element={<QuadroLayout />}>
          <Route index element={<Navigate to="circuitos" replace />} />
          <Route path="novo" element={<NovoCircuito />} />
          <Route path="circuitos" element={<Circuitos />} />
          <Route path="circuitos/:cid" element={<DetalheCircuito />} />
          <Route path="circuitos/:cid/editar" element={<NovoCircuito />} />
          <Route path="resumo" element={<Resumo />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

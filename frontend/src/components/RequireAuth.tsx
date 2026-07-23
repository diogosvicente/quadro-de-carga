import { Navigate, Outlet } from 'react-router-dom';
import { estaAutenticado } from '../auth';

/**
 * Guard de rota: sem sessão ativa, redireciona para /login; com sessão, renderiza
 * as rotas filhas (Outlet). Envolve todas as rotas do app menos /login.
 */
export function RequireAuth() {
  if (!estaAutenticado()) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}

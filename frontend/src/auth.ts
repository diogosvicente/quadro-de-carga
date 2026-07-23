/**
 * Autenticação ilustrativa (fake) — não há backend de login. A sessão vive só no
 * localStorage; o guard de rota (RequireAuth) apenas exige que exista um usuário.
 * Quando a autenticação real for desenvolvida, trocar este módulo pela integração.
 */

const CHAVE = 'nbr5410.auth';

interface Sessao {
  usuario: string;
}

/** Nome do usuário da sessão atual, ou '' quando não há sessão. */
export function usuarioAtual(): string {
  try {
    const bruto = localStorage.getItem(CHAVE);
    if (!bruto) return '';
    const dados = JSON.parse(bruto) as Partial<Sessao>;
    return typeof dados.usuario === 'string' ? dados.usuario : '';
  } catch {
    return '';
  }
}

/** Há uma sessão ativa? */
export function estaAutenticado(): boolean {
  return usuarioAtual() !== '';
}

/** Inicia a sessão (login fake). Vazio vira 'Convidado' para nunca ficar sem nome. */
export function entrar(usuario: string): void {
  const nome = usuario.trim() === '' ? 'Convidado' : usuario.trim();
  const sessao: Sessao = { usuario: nome };
  localStorage.setItem(CHAVE, JSON.stringify(sessao));
}

/** Encerra a sessão. */
export function sair(): void {
  localStorage.removeItem(CHAVE);
}

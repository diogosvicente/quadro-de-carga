import type { ErroApi } from '../types/comum';

/**
 * Erro lançado quando a API responde fora da faixa 2xx.
 * Carrega o envelope de erro do backend: status, mensagem e erros por campo.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly mensagem: string;
  readonly erros: Record<string, string> | null;

  constructor(status: number, mensagem: string, erros: Record<string, string> | null) {
    super(mensagem);
    this.name = 'ApiError';
    this.status = status;
    this.mensagem = mensagem;
    this.erros = erros;
  }
}

function ehEnvelopeErro(corpo: unknown): corpo is ErroApi {
  return (
    typeof corpo === 'object' &&
    corpo !== null &&
    'status' in corpo &&
    'mensagem' in corpo &&
    typeof (corpo as { mensagem: unknown }).mensagem === 'string'
  );
}

/** Único ponto do app que conhece fetch, baseURL e o envelope de erro. */
export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  let resposta: Response;
  try {
    resposta = await fetch(path, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    });
  } catch {
    throw new ApiError(0, 'Falha de rede: não foi possível contatar o servidor.', null);
  }

  if (resposta.status === 204) {
    return undefined as unknown as T;
  }

  let corpo: unknown = null;
  try {
    corpo = await resposta.json();
  } catch {
    corpo = null;
  }

  if (!resposta.ok) {
    if (ehEnvelopeErro(corpo)) {
      throw new ApiError(corpo.status, corpo.mensagem, corpo.erros);
    }
    throw new ApiError(resposta.status, `Erro ${resposta.status} ao chamar a API.`, null);
  }

  return corpo as T;
}

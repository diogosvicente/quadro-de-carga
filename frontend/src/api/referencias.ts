import { api } from './client';
import type { ReferenciasResponse } from '../types/referencias';

let cache: Promise<ReferenciasResponse> | null = null;

/** Referências para selects — imutáveis durante a sessão, buscadas uma única vez. */
export function obterReferencias(): Promise<ReferenciasResponse> {
  if (cache === null) {
    cache = api<ReferenciasResponse>('/api/referencias').catch((erro: unknown) => {
      cache = null; // permite tentar de novo após falha
      throw erro;
    });
  }
  return cache;
}

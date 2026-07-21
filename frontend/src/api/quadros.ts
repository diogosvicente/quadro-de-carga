import { api } from './client';
import type { QuadroRequest, QuadroResponse, ResumoQuadroResponse } from '../types/quadro';

export const listarQuadros = () => api<QuadroResponse[]>('/api/quadros');

export const criarQuadro = (corpo: QuadroRequest) =>
  api<QuadroResponse>('/api/quadros', { method: 'POST', body: JSON.stringify(corpo) });

export const buscarQuadro = (id: number) => api<QuadroResponse>(`/api/quadros/${id}`);

export const atualizarQuadro = (id: number, corpo: QuadroRequest) =>
  api<QuadroResponse>(`/api/quadros/${id}`, { method: 'PUT', body: JSON.stringify(corpo) });

export const removerQuadro = (id: number) =>
  api<void>(`/api/quadros/${id}`, { method: 'DELETE' });

export const buscarResumoQuadro = (id: number) =>
  api<ResumoQuadroResponse>(`/api/quadros/${id}/resumo`);

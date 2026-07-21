import { api } from './client';
import type { CircuitoRequest, CircuitoResponse, ResultadoCircuito } from '../types/circuito';

export const listarCircuitos = (quadroId: number) =>
  api<CircuitoResponse[]>(`/api/quadros/${quadroId}/circuitos`);

export const criarCircuito = (quadroId: number, corpo: CircuitoRequest) =>
  api<CircuitoResponse>(`/api/quadros/${quadroId}/circuitos`, {
    method: 'POST',
    body: JSON.stringify(corpo),
  });

export const buscarCircuito = (quadroId: number, circuitoId: number) =>
  api<CircuitoResponse>(`/api/quadros/${quadroId}/circuitos/${circuitoId}`);

export const atualizarCircuito = (quadroId: number, circuitoId: number, corpo: CircuitoRequest) =>
  api<CircuitoResponse>(`/api/quadros/${quadroId}/circuitos/${circuitoId}`, {
    method: 'PUT',
    body: JSON.stringify(corpo),
  });

export const removerCircuito = (quadroId: number, circuitoId: number) =>
  api<void>(`/api/quadros/${quadroId}/circuitos/${circuitoId}`, { method: 'DELETE' });

/** Prévia do dimensionamento sem persistir (POST /api/calculos/circuito). */
export const calcularCircuito = (corpo: CircuitoRequest) =>
  api<ResultadoCircuito>('/api/calculos/circuito', {
    method: 'POST',
    body: JSON.stringify(corpo),
  });

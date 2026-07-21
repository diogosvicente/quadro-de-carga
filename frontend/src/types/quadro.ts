import type { Fases, MetodoInstalacao, SistemaTensao } from './comum';
import type { CircuitoResponse, DisjuntorRecomendado } from './circuito';

export interface QuadroRequest {
  nome: string;
  local?: string;
  sistemaTensao: SistemaTensao;
  fasesAlimentador: Fases;
  fatorDemanda: number;
  cargaReservaVA: number;
  comprimentoM: number;
  metodoInstalacao: MetodoInstalacao;
  quedaAdmissivelPct: number;
  temperaturaC: number;
}

export interface QuadroResponse {
  id: number;
  nome: string;
  local: string | null;
  sistemaTensao: SistemaTensao;
  fasesAlimentador: Fases;
  fatorDemanda: number;
  cargaReservaVA: number;
  comprimentoM: number;
  metodoInstalacao: MetodoInstalacao;
  quedaAdmissivelPct: number;
  temperaturaC: number;
  totalCircuitos: number;
  potenciaTotalW: number;
}

export interface ResumoAlimentador {
  potenciaTotalW: number;
  potenciaTotalVA: number;
  cargaDemandadaVA: number;
  correnteTotalA: number;
  tensaoV: number;
  fases: number;
  disjuntorGeral: DisjuntorRecomendado;
  correnteCalculadaA: number;
  maiorDisjuntorCircuitoA: number | null;
  secaoAlimentadorMm2: number;
  secaoPorCapacidadeMm2: number;
  secaoMinimaPorRegraMm2: number | null;
  maiorSecaoCircuitoMm2: number | null;
  secaoNeutroMm2: number;
  secaoTerraMm2: number;
  rotuloCabo: string;
  quedaCalculadaPct: number;
  capacidadeQuadroVA: number;
}

export interface ResumoQuadroResponse {
  quadro: QuadroResponse;
  /** null quando o quadro não tem circuitos. */
  alimentador: ResumoAlimentador | null;
  circuitos: CircuitoResponse[];
}

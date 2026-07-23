import type { Fases, Isolante, MetodoInstalacao, TensaoCircuito, TipoCircuito } from './comum';

export interface CircuitoRequest {
  numero: number;
  descricao?: string;
  tipo: TipoCircuito;
  tensaoV: TensaoCircuito;
  fases: Fases;
  potenciaW: number;
  fatorPotencia: number;
  comprimentoM: number;
  circuitosAgrupados: number;
  circuitosParalelos: number;
  temperaturaC: number;
  metodoInstalacao?: MetodoInstalacao;
  isolante?: Isolante;
  formaAgrupamentoRef?: number;
  fatorDemanda?: number;
  quedaAdmissivelPct?: number;
  linhaSubterranea?: boolean;
}

export interface DisjuntorRecomendado {
  polos: number;
  correnteA: number;
  rotulo: string;
}

export interface ResultadoCircuito {
  potenciaVA: number;
  correnteProjetoA: number;
  correnteCorrigidaA: number;
  fatorAgrupamento: number;
  fatorTemperatura: number;
  fatorTotal: number;
  disjuntor: DisjuntorRecomendado;
  secaoSobrecorrenteMm2: number;
  secaoQuedaCalculadaMm2: number;
  secaoQuedaMm2: number;
  secaoMinimaMm2: number;
  secaoFinalMm2: number;
  circuitosParalelos: number;
  secaoFaseParaleloMm2: number;
  secaoNeutroMm2: number;
  secaoTerraMm2: number;
  rotuloCabo: string;
  quedaCalculadaPct: number;
}

/** Todos os campos do request (com defaults preenchidos) + id, quadroId e resultado. */
export interface CircuitoResponse {
  id: number;
  quadroId: number;
  numero: number;
  descricao: string | null;
  tipo: TipoCircuito;
  tensaoV: TensaoCircuito;
  fases: Fases;
  potenciaW: number;
  fatorPotencia: number;
  comprimentoM: number;
  circuitosAgrupados: number;
  circuitosParalelos: number;
  temperaturaC: number;
  metodoInstalacao: MetodoInstalacao;
  isolante: Isolante;
  formaAgrupamentoRef: number;
  fatorDemanda: number;
  quedaAdmissivelPct: number;
  linhaSubterranea: boolean;
  resultado: ResultadoCircuito;
}

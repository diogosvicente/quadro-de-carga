import type { Isolante, MetodoInstalacao, SistemaTensao, TipoCircuito } from './comum';

export interface FaseRef {
  valor: number;
  rotulo: string;
}

export interface TipoCircuitoRef {
  codigo: TipoCircuito;
  rotulo: string;
  secaoMinimaMm2: number;
}

export interface IsolanteRef {
  codigo: Isolante;
  rotulo: string;
}

export interface MetodoInstalacaoRef {
  codigo: MetodoInstalacao;
  descricao: string;
}

export interface SistemaTensaoRef {
  codigo: SistemaTensao;
  faseNeutroV: number;
  faseFaseV: number;
  rotulo: string;
}

export interface FormaAgrupamentoRef {
  ref: number;
  descricao: string;
}

/** Listas para popular todos os selects do app. */
export interface ReferenciasResponse {
  tensoes: number[];
  fases: FaseRef[];
  tiposCircuito: TipoCircuitoRef[];
  isolantes: IsolanteRef[];
  metodosInstalacao: MetodoInstalacaoRef[];
  sistemasTensao: SistemaTensaoRef[];
  formasAgrupamento: FormaAgrupamentoRef[];
}

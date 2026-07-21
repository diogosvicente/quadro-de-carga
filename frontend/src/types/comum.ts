/** Enums e tipos comuns do contrato da API (congelado). */

export type TipoCircuito = 'ILUMINACAO' | 'FORCA' | 'SINALIZACAO';

export type Isolante = 'PVC' | 'EPR';

export type MetodoInstalacao = 'A1' | 'A2' | 'B1' | 'B2' | 'C' | 'D' | 'E' | 'F' | 'G';

export type SistemaTensao = 'V127_220' | 'V220_380' | 'V254_440';

export type TensaoCircuito = 127 | 220 | 380 | 440 | 660;

export type Fases = 1 | 2 | 3;

/** Envelope de erro devolvido pela API (HTTP 400/404/422/500). */
export interface ErroApi {
  status: number;
  mensagem: string;
  erros: Record<string, string> | null;
}

/** Formatação de números em pt-BR (vírgula decimal) via Intl.NumberFormat. */

const formatadores = new Map<string, Intl.NumberFormat>();

function nf(casasMin: number, casasMax: number): Intl.NumberFormat {
  const chave = `${casasMin}:${casasMax}`;
  let formatador = formatadores.get(chave);
  if (!formatador) {
    formatador = new Intl.NumberFormat('pt-BR', {
      minimumFractionDigits: casasMin,
      maximumFractionDigits: casasMax,
    });
    formatadores.set(chave, formatador);
  }
  return formatador;
}

export function num(valor: number, casasMax = 2, casasMin = 0): string {
  return nf(casasMin, casasMax).format(valor);
}

export function amp(valor: number): string {
  return `${num(valor, 1)} A`;
}

export function mm2(valor: number): string {
  return `${num(valor, 2)} mm²`;
}

export function watts(valor: number): string {
  return `${num(valor, 0)} W`;
}

export function kw(valorW: number): string {
  return `${num(valorW / 1000, 2)} kW`;
}

export function kva(valorVA: number): string {
  return `${num(valorVA / 1000, 2)} kVA`;
}

export function pct(valor: number): string {
  return `${num(valor, 2)}%`;
}

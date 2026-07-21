import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiError } from '../api/client';
import { atualizarQuadro, criarQuadro } from '../api/quadros';
import type { MetodoInstalacao, SistemaTensao } from '../types/comum';
import type { QuadroRequest, QuadroResponse } from '../types/quadro';
import type { ReferenciasResponse } from '../types/referencias';
import { Alerta } from './Alerta';
import { Campo } from './Campo';

interface FormQuadro {
  nome: string;
  local: string;
  sistemaTensao: SistemaTensao;
  fasesAlimentador: string;
  fatorDemanda: string;
  cargaReservaVA: string;
  comprimentoM: string;
  metodoInstalacao: MetodoInstalacao;
  quedaAdmissivelPct: string;
  temperaturaC: string;
}

// Defaults do contrato: V127_220, 3, 1, 0, 10, "B1", 2, 30.
const FORM_PADRAO: FormQuadro = {
  nome: '',
  local: '',
  sistemaTensao: 'V127_220',
  fasesAlimentador: '3',
  fatorDemanda: '1',
  cargaReservaVA: '0',
  comprimentoM: '10',
  metodoInstalacao: 'B1',
  quedaAdmissivelPct: '2',
  temperaturaC: '30',
};

function deResposta(quadro: QuadroResponse): FormQuadro {
  return {
    nome: quadro.nome,
    local: quadro.local ?? '',
    sistemaTensao: quadro.sistemaTensao,
    fasesAlimentador: String(quadro.fasesAlimentador),
    fatorDemanda: String(quadro.fatorDemanda),
    cargaReservaVA: String(quadro.cargaReservaVA),
    comprimentoM: String(quadro.comprimentoM),
    metodoInstalacao: quadro.metodoInstalacao,
    quedaAdmissivelPct: String(quadro.quedaAdmissivelPct),
    temperaturaC: String(quadro.temperaturaC),
  };
}

function paraNumero(texto: string): number {
  return Number(texto.trim().replace(',', '.'));
}

interface QuadroFormProps {
  /** Quando presente, o formulário edita (PUT); caso contrário, cria (POST). */
  inicial?: QuadroResponse;
  referencias: ReferenciasResponse;
  aoSalvar: (quadro: QuadroResponse) => void;
  aoCancelar: () => void;
}

/** Formulário de quadro (alimentador geral) — criação e edição. */
export function QuadroForm({ inicial, referencias, aoSalvar, aoCancelar }: QuadroFormProps) {
  const [form, setForm] = useState<FormQuadro>(() =>
    inicial ? deResposta(inicial) : FORM_PADRAO,
  );
  const [erros, setErros] = useState<Record<string, string>>({});
  const [mensagem, setMensagem] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  const alterar = <K extends keyof FormQuadro>(campo: K, valor: FormQuadro[K]) => {
    setForm((atual) => ({ ...atual, [campo]: valor }) as FormQuadro);
  };

  const montar = (): QuadroRequest | null => {
    const locais: Record<string, string> = {};
    const nome = form.nome.trim();
    if (nome === '') locais.nome = 'Informe o nome do quadro.';
    const fatorDemanda = paraNumero(form.fatorDemanda);
    if (Number.isNaN(fatorDemanda) || fatorDemanda <= 0 || fatorDemanda > 1) {
      locais.fatorDemanda = 'Fator de demanda deve estar entre 0 e 1.';
    }
    const cargaReservaVA = paraNumero(form.cargaReservaVA);
    if (Number.isNaN(cargaReservaVA) || cargaReservaVA < 0) {
      locais.cargaReservaVA = 'Informe a carga de reserva em VA (0 ou mais).';
    }
    const comprimentoM = paraNumero(form.comprimentoM);
    if (Number.isNaN(comprimentoM) || comprimentoM <= 0) {
      locais.comprimentoM = 'Informe o comprimento em metros (maior que zero).';
    }
    const quedaAdmissivelPct = paraNumero(form.quedaAdmissivelPct);
    if (Number.isNaN(quedaAdmissivelPct) || quedaAdmissivelPct <= 0) {
      locais.quedaAdmissivelPct = 'Informe a queda admissível em % (maior que zero).';
    }
    const temperaturaC = paraNumero(form.temperaturaC);
    if (Number.isNaN(temperaturaC)) {
      locais.temperaturaC = 'Informe a temperatura em °C.';
    }
    if (Object.keys(locais).length > 0) {
      setErros(locais);
      setMensagem('Corrija os campos destacados.');
      return null;
    }
    setErros({});
    setMensagem(null);
    const local = form.local.trim();
    return {
      nome,
      local: local === '' ? undefined : local,
      sistemaTensao: form.sistemaTensao,
      fasesAlimentador: paraNumero(form.fasesAlimentador) as QuadroRequest['fasesAlimentador'],
      fatorDemanda,
      cargaReservaVA,
      comprimentoM,
      metodoInstalacao: form.metodoInstalacao,
      quedaAdmissivelPct,
      temperaturaC,
    };
  };

  const enviar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const corpo = montar();
    if (!corpo) return;
    setSalvando(true);
    const chamada = inicial ? atualizarQuadro(inicial.id, corpo) : criarQuadro(corpo);
    chamada
      .then((quadro) => {
        aoSalvar(quadro);
      })
      .catch((erro: unknown) => {
        if (erro instanceof ApiError) {
          setMensagem(erro.mensagem);
          setErros(erro.erros ?? {});
        } else {
          setMensagem(erro instanceof Error ? erro.message : 'Erro inesperado ao salvar.');
          setErros({});
        }
        setSalvando(false);
      });
  };

  return (
    <section className="cartao">
      <h2>{inicial ? 'Editar Quadro' : 'Novo Quadro'}</h2>
      {mensagem ? <Alerta>{mensagem}</Alerta> : null}
      <form onSubmit={enviar} noValidate>
        <div className="form-grade">
          <Campo id="nome" rotulo="Nome do Quadro *" erro={erros.nome} cheio>
            <input
              id="nome"
              type="text"
              value={form.nome}
              onChange={(e) => alterar('nome', e.target.value)}
              placeholder="Ex: QDC Bloco D"
              maxLength={120}
            />
          </Campo>
          <Campo id="local" rotulo="Local" erro={erros.local} cheio>
            <input
              id="local"
              type="text"
              value={form.local}
              onChange={(e) => alterar('local', e.target.value)}
              placeholder="Ex: Dept. Engenharia Elétrica"
              maxLength={120}
            />
          </Campo>
          <Campo id="sistemaTensao" rotulo="Sistema de Tensão" erro={erros.sistemaTensao}>
            <select
              id="sistemaTensao"
              value={form.sistemaTensao}
              onChange={(e) => alterar('sistemaTensao', e.target.value as SistemaTensao)}
            >
              {referencias.sistemasTensao.map((s) => (
                <option key={s.codigo} value={s.codigo}>
                  {s.rotulo}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="fasesAlimentador" rotulo="Fases do Alimentador" erro={erros.fasesAlimentador}>
            <select
              id="fasesAlimentador"
              value={form.fasesAlimentador}
              onChange={(e) => alterar('fasesAlimentador', e.target.value)}
            >
              {referencias.fases.map((f) => (
                <option key={f.valor} value={String(f.valor)}>
                  {f.rotulo}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="fatorDemanda" rotulo="Fator de Demanda" erro={erros.fatorDemanda}>
            <input
              id="fatorDemanda"
              type="number"
              inputMode="decimal"
              step="0.01"
              min="0.01"
              max="1"
              value={form.fatorDemanda}
              onChange={(e) => alterar('fatorDemanda', e.target.value)}
            />
          </Campo>
          <Campo id="cargaReservaVA" rotulo="Carga de Reserva (VA)" erro={erros.cargaReservaVA}>
            <input
              id="cargaReservaVA"
              type="number"
              inputMode="decimal"
              step="1"
              min="0"
              value={form.cargaReservaVA}
              onChange={(e) => alterar('cargaReservaVA', e.target.value)}
            />
          </Campo>
          <Campo id="comprimentoM" rotulo="Comprimento do Alimentador (m)" erro={erros.comprimentoM}>
            <input
              id="comprimentoM"
              type="number"
              inputMode="decimal"
              step="0.1"
              min="0.1"
              value={form.comprimentoM}
              onChange={(e) => alterar('comprimentoM', e.target.value)}
            />
          </Campo>
          <Campo id="metodoInstalacao" rotulo="Método de Instalação" erro={erros.metodoInstalacao}>
            <select
              id="metodoInstalacao"
              value={form.metodoInstalacao}
              onChange={(e) => alterar('metodoInstalacao', e.target.value as MetodoInstalacao)}
            >
              {referencias.metodosInstalacao.map((m) => (
                <option key={m.codigo} value={m.codigo}>
                  {m.codigo} — {m.descricao}
                </option>
              ))}
            </select>
          </Campo>
          <Campo id="quedaAdmissivelPct" rotulo="Queda Admissível (%)" erro={erros.quedaAdmissivelPct}>
            <input
              id="quedaAdmissivelPct"
              type="number"
              inputMode="decimal"
              step="0.1"
              min="0.1"
              value={form.quedaAdmissivelPct}
              onChange={(e) => alterar('quedaAdmissivelPct', e.target.value)}
            />
          </Campo>
          <Campo id="temperaturaC" rotulo="Temperatura Ambiente (°C)" erro={erros.temperaturaC}>
            <input
              id="temperaturaC"
              type="number"
              inputMode="decimal"
              step="1"
              value={form.temperaturaC}
              onChange={(e) => alterar('temperaturaC', e.target.value)}
            />
          </Campo>
        </div>
        <div className="form-acoes">
          <button type="button" className="botao-sec" onClick={aoCancelar} disabled={salvando}>
            Cancelar
          </button>
          <button type="submit" className="botao" disabled={salvando}>
            {salvando ? 'Salvando…' : inicial ? 'Salvar Alterações' : 'Criar Quadro'}
          </button>
        </div>
      </form>
    </section>
  );
}
